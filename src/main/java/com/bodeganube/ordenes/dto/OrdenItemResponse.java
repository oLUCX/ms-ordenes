package com.bodeganube.ordenes.dto;

import com.bodeganube.ordenes.model.OrdenItem;

/** Item de una orden tal como lo devuelve la API (sin la referencia de vuelta a la orden). */
public record OrdenItemResponse(
        Long id,
        String productoId,
        Integer cantidad
) {
    public static OrdenItemResponse de(OrdenItem item) {
        return new OrdenItemResponse(item.getId(), item.getProductoId(), item.getCantidad());
    }
}
