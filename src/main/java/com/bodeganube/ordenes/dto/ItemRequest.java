package com.bodeganube.ordenes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ItemRequest(
        @NotBlank String productoId,
        @Positive Integer cantidad
) {
}
