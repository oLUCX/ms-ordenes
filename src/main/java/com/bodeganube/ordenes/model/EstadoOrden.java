package com.bodeganube.ordenes.model;

/**
 * Ciclo de vida de una orden. Regla de negocio del caso: si no hay stock disponible, la orden
 * NO queda habilitada para picking.
 *
 * PENDIENTE_STOCK -> LISTA_PARA_PICKING -> EN_PICKING -> DESPACHADA
 *                 -> RECHAZADA_SIN_STOCK
 */
public enum EstadoOrden {
    PENDIENTE_STOCK,
    LISTA_PARA_PICKING,
    EN_PICKING,
    DESPACHADA,
    RECHAZADA_SIN_STOCK;

    /** Indica si desde este estado se puede pasar al nuevo. DESPACHADA y RECHAZADA_SIN_STOCK son finales. */
    public boolean puedeCambiarA(EstadoOrden nuevo) {
        return switch (this) {
            case PENDIENTE_STOCK -> nuevo == LISTA_PARA_PICKING || nuevo == RECHAZADA_SIN_STOCK;
            case LISTA_PARA_PICKING -> nuevo == EN_PICKING;
            case EN_PICKING -> nuevo == DESPACHADA;
            case DESPACHADA, RECHAZADA_SIN_STOCK -> false;
        };
    }
}
