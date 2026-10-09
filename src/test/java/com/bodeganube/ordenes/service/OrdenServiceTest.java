package com.bodeganube.ordenes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bodeganube.ordenes.dto.ActualizarOrdenRequest;
import com.bodeganube.ordenes.dto.CrearOrdenRequest;
import com.bodeganube.ordenes.dto.ItemRequest;
import com.bodeganube.ordenes.dto.OrdenRegistrada;
import com.bodeganube.ordenes.dto.OrdenResponse;
import com.bodeganube.ordenes.exception.RecursoNoEncontradoException;
import com.bodeganube.ordenes.exception.ReglaNegocioException;
import com.bodeganube.ordenes.model.EstadoOrden;
import com.bodeganube.ordenes.model.Orden;
import com.bodeganube.ordenes.repository.OrdenRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Pruebas unitarias de idempotencia y del ciclo de vida de la orden, con el repositorio simulado. */
@ExtendWith(MockitoExtension.class)
class OrdenServiceTest {

    @Mock
    private OrdenRepository ordenRepository;

    @InjectMocks
    private OrdenService ordenService;

    @Test
    void avisoNuevoCreaOrdenPendienteDeStockConSusItems() {
        when(ordenRepository.findByExternalOrderId("shopify-001")).thenReturn(Optional.empty());
        when(ordenRepository.save(any(Orden.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        OrdenRegistrada registro = ordenService.recibirAvisoDeVenta(aviso("shopify-001"));

        assertThat(registro.creada()).isTrue();
        assertThat(registro.orden().estado()).isEqualTo(EstadoOrden.PENDIENTE_STOCK);
        assertThat(registro.orden().items()).hasSize(2);
    }

    @Test
    void avisoRepetidoDevuelveLaOrdenExistenteSinCrearOtra() {
        when(ordenRepository.findByExternalOrderId("shopify-001"))
                .thenReturn(Optional.of(orden(1L, EstadoOrden.PENDIENTE_STOCK)));

        OrdenRegistrada registro = ordenService.recibirAvisoDeVenta(aviso("shopify-001"));

        assertThat(registro.creada()).isFalse();
        assertThat(registro.orden().id()).isEqualTo(1L);
        verify(ordenRepository, never()).save(any());
    }

    @Test
    void cambiarEstadoPermitidoActualizaLaOrden() {
        when(ordenRepository.findById(1L)).thenReturn(Optional.of(orden(1L, EstadoOrden.PENDIENTE_STOCK)));
        when(ordenRepository.save(any(Orden.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        OrdenResponse actualizada = ordenService.cambiarEstado(1L, EstadoOrden.LISTA_PARA_PICKING);

        assertThat(actualizada.estado()).isEqualTo(EstadoOrden.LISTA_PARA_PICKING);
    }

    @Test
    void cambiarEstadoNoPermitidoLanzaReglaNegocio() {
        when(ordenRepository.findById(1L)).thenReturn(Optional.of(orden(1L, EstadoOrden.DESPACHADA)));

        assertThatThrownBy(() -> ordenService.cambiarEstado(1L, EstadoOrden.PENDIENTE_STOCK))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("DESPACHADA");
        verify(ordenRepository, never()).save(any());
    }

    @Test
    void actualizarItemsDeOrdenEnPickingLanzaReglaNegocio() {
        when(ordenRepository.findById(1L)).thenReturn(Optional.of(orden(1L, EstadoOrden.EN_PICKING)));
        ActualizarOrdenRequest request = new ActualizarOrdenRequest(List.of(new ItemRequest("SKU-1", 1)));

        assertThatThrownBy(() -> ordenService.actualizarItems(1L, request))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void eliminarOrdenDespachadaLanzaReglaNegocio() {
        when(ordenRepository.findById(1L)).thenReturn(Optional.of(orden(1L, EstadoOrden.DESPACHADA)));

        assertThatThrownBy(() -> ordenService.eliminar(1L))
                .isInstanceOf(ReglaNegocioException.class);
        verify(ordenRepository, never()).delete(any());
    }

    @Test
    void obtenerOrdenInexistenteLanzaRecursoNoEncontrado() {
        when(ordenRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ordenService.obtener(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("99");
    }

    @Test
    void lasOrdenesFinalizadasNoCambianDeEstado() {
        assertThat(EstadoOrden.DESPACHADA.puedeCambiarA(EstadoOrden.EN_PICKING)).isFalse();
        assertThat(EstadoOrden.RECHAZADA_SIN_STOCK.puedeCambiarA(EstadoOrden.LISTA_PARA_PICKING)).isFalse();
        assertThat(EstadoOrden.EN_PICKING.puedeCambiarA(EstadoOrden.DESPACHADA)).isTrue();
    }

    private static CrearOrdenRequest aviso(String externalOrderId) {
        return new CrearOrdenRequest(externalOrderId, "comercio-123",
                List.of(new ItemRequest("SKU-1", 2), new ItemRequest("SKU-2", 1)));
    }

    private static Orden orden(Long id, EstadoOrden estado) {
        Orden orden = new Orden();
        orden.setId(id);
        orden.setExternalOrderId("shopify-001");
        orden.setComercioId("comercio-123");
        orden.setEstado(estado);
        orden.setFechaCreacion(LocalDateTime.now());
        return orden;
    }
}
