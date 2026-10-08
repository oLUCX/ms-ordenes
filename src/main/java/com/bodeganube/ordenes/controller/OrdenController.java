package com.bodeganube.ordenes.controller;

import com.bodeganube.ordenes.dto.CrearOrdenRequest;
import com.bodeganube.ordenes.dto.OrdenRegistrada;
import com.bodeganube.ordenes.dto.OrdenResponse;
import com.bodeganube.ordenes.model.EstadoOrden;
import com.bodeganube.ordenes.service.OrdenService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * RF-02: recepcion de avisos de venta externos via webhook.
 * RF-03: consulta de ordenes disponibles para picking.
 * RF-06: consulta exclusiva de ordenes propias por comercio (Tenant Isolation).
 */
@RestController
@RequestMapping("/api/ordenes")
public class OrdenController {

    private final OrdenService ordenService;

    public OrdenController(OrdenService ordenService) {
        this.ordenService = ordenService;
    }

    @PostMapping
    public ResponseEntity<OrdenResponse> recibirAvisoDeVenta(@Valid @RequestBody CrearOrdenRequest request) {
        OrdenRegistrada registro = ordenService.recibirAvisoDeVenta(request);
        if (!registro.creada()) {
            // Reintento del mismo evento: se responde con la orden ya registrada, sin duplicarla
            return ResponseEntity.ok(registro.orden());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(registro.orden());
    }

    @GetMapping
    public List<OrdenResponse> consultarOrdenesPropias(@RequestParam String comercioId) {
        return ordenService.consultarPorComercio(comercioId);
    }

    @GetMapping("/disponibles-picking")
    public List<OrdenResponse> ordenesDisponiblesParaPicking() {
        return ordenService.disponiblesParaPicking();
    }

    @PatchMapping("/{id}/estado")
    public OrdenResponse actualizarEstado(@PathVariable Long id, @RequestParam EstadoOrden nuevoEstado) {
        return ordenService.actualizarEstado(id, nuevoEstado);
    }
}
