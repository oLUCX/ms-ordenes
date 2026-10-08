package com.bodeganube.ordenes.service;

import com.bodeganube.ordenes.dto.CrearOrdenRequest;
import com.bodeganube.ordenes.dto.OrdenRegistrada;
import com.bodeganube.ordenes.dto.OrdenResponse;
import com.bodeganube.ordenes.exception.RecursoNoEncontradoException;
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

        request.items().forEach(itemReq -> {
            OrdenItem item = new OrdenItem();
            item.setOrden(orden);
            item.setProductoId(itemReq.productoId());
            item.setCantidad(itemReq.cantidad());
            orden.getItems().add(item);
        });

        // En la version con AWS, aqui se invoca a ms-inventario (Circuit Breaker + Retry) para reservar
        // stock antes de pasar la orden a LISTA_PARA_PICKING. Ver seccion 3.2 del informe de arquitectura.
        return new OrdenRegistrada(OrdenResponse.de(ordenRepository.save(orden)), true);
    }

    /** RF-06: cada comercio consulta solo sus propias ordenes (Tenant Isolation). */
    public List<OrdenResponse> consultarPorComercio(String comercioId) {
        return ordenRepository.findByComercioId(comercioId).stream()
                .map(OrdenResponse::de)
                .toList();
    }

    /** RF-03: ordenes con stock ya reservado, listas para que bodega las prepare. */
    public List<OrdenResponse> disponiblesParaPicking() {
        return ordenRepository.findByEstado(EstadoOrden.LISTA_PARA_PICKING).stream()
                .map(OrdenResponse::de)
                .toList();
    }

    @Transactional
    public OrdenResponse actualizarEstado(Long id, EstadoOrden nuevoEstado) {
        Orden orden = buscar(id);
        orden.setEstado(nuevoEstado);
        return OrdenResponse.de(ordenRepository.save(orden));
    }

    private Orden buscar(Long id) {
        return ordenRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la orden " + id));
    }
}
