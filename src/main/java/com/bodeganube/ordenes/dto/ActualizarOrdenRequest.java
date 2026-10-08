package com.bodeganube.ordenes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Reemplaza los items de una orden que todavia no tiene stock reservado. */
public record ActualizarOrdenRequest(
        @NotEmpty(message = "La orden debe tener al menos un item")
        List<@Valid ItemRequest> items
) {
}
