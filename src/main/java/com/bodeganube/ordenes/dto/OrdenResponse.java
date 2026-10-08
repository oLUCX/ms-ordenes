package com.bodeganube.ordenes.dto;

import com.bodeganube.ordenes.model.EstadoOrden;
import com.bodeganube.ordenes.model.Orden;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lo que la API devuelve de una orden. Devolver la entidad JPA directamente hacia que Jackson
 * entrara en un ciclo infinito (Orden -> items -> orden -> items...); este DTO lo corta.
 */
public record OrdenResponse(
        Long id,
        String externalOrderId,
        String comercioId,
        EstadoOrden estado,
        String trackingNumber,
        LocalDateTime fechaCreacion,
        List<OrdenItemResponse> items
) {
    public static OrdenResponse de(Orden orden) {
        List<OrdenItemResponse> items = orden.getItems().stream()
                .map(OrdenItemResponse::de)
                .toList();
        return new OrdenResponse(orden.getId(), orden.getExternalOrderId(), orden.getComercioId(),
                orden.getEstado(), orden.getTrackingNumber(), orden.getFechaCreacion(), items);
    }
}
