package com.bodeganube.ordenes.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bodeganube.ordenes.dto.CrearOrdenRequest;
import com.bodeganube.ordenes.dto.OrdenItemResponse;
import com.bodeganube.ordenes.dto.OrdenRegistrada;
import com.bodeganube.ordenes.dto.OrdenResponse;
import com.bodeganube.ordenes.exception.RecursoNoEncontradoException;
import com.bodeganube.ordenes.exception.ReglaNegocioException;
import com.bodeganube.ordenes.model.EstadoOrden;
import com.bodeganube.ordenes.service.OrdenService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Prueba la capa web aislada: codigos HTTP, validaciones y formato de error. */
@WebMvcTest(OrdenController.class)
class OrdenControllerTest {

    private static final String AVISO = """
            {"externalOrderId":"shopify-001","comercioId":"comercio-123",
             "items":[{"productoId":"SKU-1","cantidad":2}]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrdenService ordenService;

    @Test
    void avisoNuevoDevuelve201ConLocation() throws Exception {
        when(ordenService.recibirAvisoDeVenta(any(CrearOrdenRequest.class)))
                .thenReturn(new OrdenRegistrada(orden(EstadoOrden.PENDIENTE_STOCK), true));

        mockMvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON).content(AVISO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/ordenes/1")))
                .andExpect(jsonPath("$.items[0].productoId").value("SKU-1"));
    }

    @Test
    void avisoRepetidoDevuelve200SinDuplicar() throws Exception {
        when(ordenService.recibirAvisoDeVenta(any(CrearOrdenRequest.class)))
                .thenReturn(new OrdenRegistrada(orden(EstadoOrden.PENDIENTE_STOCK), false));

        mockMvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON).content(AVISO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void avisoSinItemsDevuelve400ConDetalle() throws Exception {
        mockMvc.perform(post("/api/ordenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalOrderId\":\"shopify-002\",\"comercioId\":\"comercio-123\",\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.items").value("La orden debe tener al menos un item"));
    }

    @Test
    void obtenerOrdenInexistenteDevuelve404() throws Exception {
        when(ordenService.obtener(99L)).thenThrow(new RecursoNoEncontradoException("No existe la orden 99"));

        mockMvc.perform(get("/api/ordenes/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No existe la orden 99"));
    }

    @Test
    void cambioDeEstadoNoPermitidoDevuelve409() throws Exception {
        when(ordenService.cambiarEstado(1L, EstadoOrden.PENDIENTE_STOCK))
                .thenThrow(new ReglaNegocioException("No se puede pasar una orden de DESPACHADA a PENDIENTE_STOCK"));

        mockMvc.perform(patch("/api/ordenes/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"PENDIENTE_STOCK\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void estadoDesconocidoDevuelve400() throws Exception {
        mockMvc.perform(patch("/api/ordenes/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"EXTRAVIADA\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eliminarDevuelve204() throws Exception {
        mockMvc.perform(delete("/api/ordenes/1"))
                .andExpect(status().isNoContent());

        verify(ordenService).eliminar(1L);
    }

    private static OrdenResponse orden(EstadoOrden estado) {
        return new OrdenResponse(1L, "shopify-001", "comercio-123", estado, null, LocalDateTime.now(),
                List.of(new OrdenItemResponse(1L, "SKU-1", 2)));
    }
}
