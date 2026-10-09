package com.bodeganube.ordenes.controller;

import com.bodeganube.ordenes.dto.ActualizarOrdenRequest;
import com.bodeganube.ordenes.dto.CambioEstadoRequest;
import com.bodeganube.ordenes.dto.CrearOrdenRequest;
import com.bodeganube.ordenes.dto.OrdenRegistrada;
import com.bodeganube.ordenes.dto.OrdenResponse;
import com.bodeganube.ordenes.service.OrdenService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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

    /** 201 si la orden es nueva; 200 con la misma orden si el evento es un reintento (idempotencia). */
    @PostMapping
    public ResponseEntity<OrdenResponse> recibirAvisoDeVenta(@Valid @RequestBody CrearOrdenRequest request) {
        OrdenRegistrada registro = ordenService.recibirAvisoDeVenta(request);
        if (!registro.creada()) {
            return ResponseEntity.ok(registro.orden());
        }
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(registro.orden().id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(registro.orden());
    }

    @GetMapping
    public List<OrdenResponse> listar(@RequestParam(required = false) String comercioId) {
        return ordenService.listar(comercioId);
    }

    @GetMapping("/{id}")
    public OrdenResponse obtener(@PathVariable Long id) {
        return ordenService.obtener(id);
    }

    @GetMapping("/disponibles-picking")
    public List<OrdenResponse> ordenesDisponiblesParaPicking() {
        return ordenService.disponiblesParaPicking();
    }

    @PutMapping("/{id}")
    public OrdenResponse actualizarItems(@PathVariable Long id, @Valid @RequestBody ActualizarOrdenRequest request) {
        return ordenService.actualizarItems(id, request);
    }

    @PatchMapping("/{id}/estado")
    public OrdenResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoRequest request) {
        return ordenService.cambiarEstado(id, request.estado());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        ordenService.eliminar(id);
    }
}
