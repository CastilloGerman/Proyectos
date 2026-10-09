package com.appgestion.api.repository;

import com.appgestion.api.domain.entity.PresupuestoSeguimientoAviso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PresupuestoSeguimientoAvisoRepository extends JpaRepository<PresupuestoSeguimientoAviso, Long>,
        PresupuestoSeguimientoAvisoRepositoryCustom {

    long countByPresupuestoId(Long presupuestoId);

    Optional<PresupuestoSeguimientoAviso> findTopByPresupuestoIdOrderByCreadoAtDesc(Long presupuestoId);

}
