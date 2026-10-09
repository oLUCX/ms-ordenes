package com.bodeganube.ordenes.dto;

import com.bodeganube.ordenes.model.EstadoOrden;
import jakarta.validation.constraints.NotNull;

/** Cuerpo del PATCH de estado, por ejemplo {"estado": "LISTA_PARA_PICKING"}. */
public record CambioEstadoRequest(
        @NotNull(message = "El nuevo estado es obligatorio")
        EstadoOrden estado
) {
}
