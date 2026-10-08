package com.bodeganube.ordenes.dto;

/**
 * Resultado de recibir un aviso de venta: la orden y si se creo ahora (true) o si el evento
 * ya se habia procesado antes y es un reintento (false). El controller responde 201 o 200 segun esto.
 */
public record OrdenRegistrada(OrdenResponse orden, boolean creada) {
}
