package com.bodeganube.ordenes.controller;

import com.bodeganube.ordenes.dto.CrearOrdenRequest;
import com.bodeganube.ordenes.model.EstadoOrden;
import com.bodeganube.ordenes.model.Orden;
import com.bodeganube.ordenes.model.OrdenItem;
import com.bodeganube.ordenes.repository.OrdenRepository;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * RF-02: recepcion de avisos de venta externos via webhook.
 * RF-03: consulta de ordenes disponibles para picking.
 * RF-06: consulta exclusiva de ordenes propias por comercio (Tenant Isolation).
 */
@RestController
@RequestMapping("/api/ordenes")
public class OrdenController {

    private final OrdenRepository ordenRepository;

    public OrdenController(OrdenRepository ordenRepository) {
        this.ordenRepository = ordenRepository;
    }

    @PostMapping
    public ResponseEntity<?> recibirAvisoDeVenta(@Valid @RequestBody CrearOrdenRequest request) {
        // Idempotent Receiver (RNF-02): si el evento ya fue procesado, se ignora el duplicado
        // sin generar una segunda orden.
        if (ordenRepository.existsByExternalOrderId(request.externalOrderId())) {
            return ResponseEntity.ok().body("Orden ya registrada previamente, se ignora el duplicado");
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

        // NOTA (esqueleto minimo): en la version funcional completa, aqui se invocaria a
        // ms-inventario (protegido con Circuit Breaker + Retry) para reservar stock antes de
        // pasar la orden a LISTA_PARA_PICKING. Ver seccion 3.2 del informe de arquitectura.
        Orden guardada = ordenRepository.save(orden);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardada);
    }

    @GetMapping
    public List<Orden> consultarOrdenesPropias(@RequestParam String comercioId) {
        return ordenRepository.findByComercioId(comercioId);
    }

    @GetMapping("/disponibles-picking")
    public List<Orden> ordenesDisponiblesParaPicking() {
        return ordenRepository.findByEstado(EstadoOrden.LISTA_PARA_PICKING);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Orden> actualizarEstado(@PathVariable Long id, @RequestParam EstadoOrden nuevoEstado) {
        Orden orden = ordenRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden no encontrada"));
        orden.setEstado(nuevoEstado);
        return ResponseEntity.ok(ordenRepository.save(orden));
    }
}
