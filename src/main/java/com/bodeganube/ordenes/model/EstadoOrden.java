package com.bodeganube.ordenes.model;

/**
 * Regla de negocio del caso: si no hay stock disponible, la orden NO queda habilitada para picking.
 */
public enum EstadoOrden {
    PENDIENTE_STOCK,
    LISTA_PARA_PICKING,
    EN_PICKING,
    DESPACHADA,
    RECHAZADA_SIN_STOCK
}
