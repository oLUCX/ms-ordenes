package com.bodeganube.ordenes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Representa el aviso de venta que llegaria desde AWS Lambda (tras pasar por API Gateway + SQS),
 * ver diagrama de arquitectura, paso 4-5.
 */
public record CrearOrdenRequest(
        @NotBlank(message = "El externalOrderId es obligatorio (es la clave de idempotencia)")
        String externalOrderId,

        @NotBlank(message = "El comercioId es obligatorio")
        String comercioId,

        @NotEmpty(message = "La orden debe tener al menos un item")
        List<@Valid ItemRequest> items
) {
}
