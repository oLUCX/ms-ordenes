package com.bodeganube.ordenes.repository;

import com.bodeganube.ordenes.model.EstadoOrden;
import com.bodeganube.ordenes.model.Orden;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdenRepository extends JpaRepository<Orden, Long> {
    boolean existsByExternalOrderId(String externalOrderId);

    List<Orden> findByComercioId(String comercioId);

    List<Orden> findByEstado(EstadoOrden estado);
}
