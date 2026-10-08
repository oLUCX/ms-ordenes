package com.bodeganube.ordenes.service;

import com.bodeganube.ordenes.dto.ActualizarOrdenRequest;
import com.bodeganube.ordenes.dto.CrearOrdenRequest;
import com.bodeganube.ordenes.dto.ItemRequest;
import com.bodeganube.ordenes.dto.OrdenRegistrada;
import com.bodeganube.ordenes.dto.OrdenResponse;
import com.bodeganube.ordenes.exception.RecursoNoEncontradoException;
import com.bodeganube.ordenes.exception.ReglaNegocioException;
import com.bodeganube.ordenes.model.EstadoOrden;
import com.bodeganube.ordenes.model.Orden;
import com.bodeganube.ordenes.model.OrdenItem;
import com.bodeganube.ordenes.repository.OrdenRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logica del ciclo de vida del pedido. El controller solo traduce HTTP y delega aqui.
 * Todas las respuestas salen como DTO, armadas dentro de la transaccion.
 */
@Service
@Transactional(readOnly = true)
public class OrdenService {

    private final OrdenRepository ordenRepository;

    public OrdenService(OrdenRepository ordenRepository) {
        this.ordenRepository = ordenRepository;
    }

    /**
     * RF-02 y RNF-02 (Idempotent Receiver): si el externalOrderId ya se proceso, devuelve la orden
     * existente sin crear otra. Asi un reintento del webhook o de SQS no duplica la venta.
     */
    @Transactional
    public OrdenRegistrada recibirAvisoDeVenta(CrearOrdenRequest request) {
        Optional<Orden> existente = ordenRepository.findByExternalOrderId(request.externalOrderId());
        if (existente.isPresent()) {
            return new OrdenRegistrada(OrdenResponse.de(existente.get()), false);
        }

        Orden orden = new Orden();
        orden.setExternalOrderId(request.externalOrderId());
        orden.setComercioId(request.comercioId());
        orden.setEstado(EstadoOrden.PENDIENTE_STOCK);
        orden.setFechaCreacion(LocalDateTime.now());
        request.items().forEach(itemReq -> orden.agregarItem(aItem(itemReq)));

        // En la version con AWS, aqui se invoca a ms-inventario (Circuit Breaker + Retry) para reservar
        // stock antes de pasar la orden a LISTA_PARA_PICKING. Ver seccion 3.2 del informe de arquitectura.
        return new OrdenRegistrada(OrdenResponse.de(ordenRepository.save(orden)), true);
    }

    /**
     * Sin comercioId lista todas (vista del operario). RF-06: con comercioId, cada comercio ve solo
     * sus propias ordenes (Tenant Isolation; el gateway lo forzara con el claim comercioId del JWT).
     */
    public List<OrdenResponse> listar(String comercioId) {
        List<Orden> ordenes = (comercioId == null || comercioId.isBlank())
                ? ordenRepository.findAll()
                : ordenRepository.findByComercioId(comercioId);
        return ordenes.stream()
                .map(OrdenResponse::de)
                .toList();
    }

    public OrdenResponse obtener(Long id) {
        return OrdenResponse.de(buscar(id));
    }

    /** RF-03: ordenes con stock ya reservado, listas para que bodega las prepare. */
    public List<OrdenResponse> disponiblesParaPicking() {
        return ordenRepository.findByEstado(EstadoOrden.LISTA_PARA_PICKING).stream()
                .map(OrdenResponse::de)
                .toList();
    }

    /** Los items solo se pueden cambiar mientras la orden espera stock; despues ya hay reservas hechas. */
    @Transactional
    public OrdenResponse actualizarItems(Long id, ActualizarOrdenRequest request) {
        Orden orden = buscar(id);
        if (orden.getEstado() != EstadoOrden.PENDIENTE_STOCK) {
            throw new ReglaNegocioException("Solo se pueden modificar los items de una orden PENDIENTE_STOCK; estado actual: "
                    + orden.getEstado());
        }
        orden.reemplazarItems(request.items().stream().map(this::aItem).toList());
        return OrdenResponse.de(ordenRepository.save(orden));
    }

    /** Aplica las transiciones definidas en EstadoOrden.puedeCambiarA. */
    @Transactional
    public OrdenResponse cambiarEstado(Long id, EstadoOrden nuevoEstado) {
        Orden orden = buscar(id);
        if (!orden.getEstado().puedeCambiarA(nuevoEstado)) {
            throw new ReglaNegocioException("No se puede pasar una orden de " + orden.getEstado() + " a " + nuevoEstado);
        }
        orden.setEstado(nuevoEstado);
        return OrdenResponse.de(ordenRepository.save(orden));
    }

    /** Una orden que ya entro a bodega (EN_PICKING o DESPACHADA) no se puede eliminar. */
    @Transactional
    public void eliminar(Long id) {
        Orden orden = buscar(id);
        if (orden.getEstado() == EstadoOrden.EN_PICKING || orden.getEstado() == EstadoOrden.DESPACHADA) {
            throw new ReglaNegocioException("No se puede eliminar la orden " + id + " porque ya esta en bodega (estado "
                    + orden.getEstado() + ")");
        }
        ordenRepository.delete(orden);
    }

    private Orden buscar(Long id) {
        return ordenRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la orden " + id));
    }

    private OrdenItem aItem(ItemRequest itemReq) {
        OrdenItem item = new OrdenItem();
        item.setProductoId(itemReq.productoId());
        item.setCantidad(itemReq.cantidad());
        return item;
    }
}
