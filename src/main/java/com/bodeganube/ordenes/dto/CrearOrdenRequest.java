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
        @NotBlank String externalOrderId,
        @NotBlank String comercioId,
        @NotEmpty List<@Valid ItemRequest> items
) {
}
