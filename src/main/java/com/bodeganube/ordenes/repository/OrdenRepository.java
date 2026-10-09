package com.bodeganube.ordenes.repository;

import com.bodeganube.ordenes.model.EstadoOrden;
import com.bodeganube.ordenes.model.Orden;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdenRepository extends JpaRepository<Orden, Long> {

    Optional<Orden> findByExternalOrderId(String externalOrderId);

    // @EntityGraph trae los items en la misma consulta (JOIN), en vez de una consulta extra por orden (N+1).
    @EntityGraph(attributePaths = "items")
    List<Orden> findByComercioId(String comercioId);

    @EntityGraph(attributePaths = "items")
    List<Orden> findByEstado(EstadoOrden estado);

    @Override
    @EntityGraph(attributePaths = "items")
    List<Orden> findAll();
}
