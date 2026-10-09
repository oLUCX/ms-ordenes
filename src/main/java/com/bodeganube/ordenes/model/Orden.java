package com.bodeganube.ordenes.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ordenes")
public class Orden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Clave de idempotencia (Idempotent Receiver): evita duplicados por reintentos del webhook externo.
    @Column(nullable = false, unique = true)
    private String externalOrderId;

    @Column(nullable = false)
    private String comercioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoOrden estado;

    private String trackingNumber;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    // Lado "uno" de la relacion: una orden tiene muchos items. La FK orden_id vive en la tabla orden_items
    // (mappedBy). cascade = ALL guarda y borra los items junto con la orden; orphanRemoval borra de la BD
    // los items que se sacan de la lista.
    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrdenItem> items = new ArrayList<>();

    /** Agrega un item manteniendo sincronizados los dos lados de la relacion OneToMany / ManyToOne. */
    public void agregarItem(OrdenItem item) {
        item.setOrden(this);
        items.add(item);
    }

    /** Reemplaza todos los items de la orden; los que se quitan se eliminan de la BD (orphanRemoval). */
    public void reemplazarItems(List<OrdenItem> nuevos) {
        items.clear();
        nuevos.forEach(this::agregarItem);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExternalOrderId() {
        return externalOrderId;
    }

    public void setExternalOrderId(String externalOrderId) {
        this.externalOrderId = externalOrderId;
    }

    public String getComercioId() {
        return comercioId;
    }

    public void setComercioId(String comercioId) {
        this.comercioId = comercioId;
    }

    public EstadoOrden getEstado() {
        return estado;
    }

    public void setEstado(EstadoOrden estado) {
        this.estado = estado;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public List<OrdenItem> getItems() {
        return items;
    }
}
