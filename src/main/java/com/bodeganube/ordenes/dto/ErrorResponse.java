package com.bodeganube.ordenes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Formato unico de error que devuelve el microservicio (ver GlobalExceptionHandler).
 * "detalles" solo aparece en errores de validacion: campo -> mensaje.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String mensaje,
        String ruta,
        Map<String, String> detalles
) {
}
