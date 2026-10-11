package com.appgestion.api.repository;

import com.appgestion.api.domain.entity.PresupuestoSeguimientoAviso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PresupuestoSeguimientoAvisoRepository extends JpaRepository<PresupuestoSeguimientoAviso, Long>,
        PresupuestoSeguimientoAvisoRepositoryCustom {

    long countByPresupuestoId(Long presupuestoId);

    Optional<PresupuestoSeguimientoAviso> findTopByPresupuestoIdOrderByCreadoAtDesc(Long presupuestoId);

    /** Fila: presupuestoId, cantidad, fecha del último aviso. */
    @Query("""
            select a.presupuesto.id, count(a), max(a.creadoAt)
            from PresupuestoSeguimientoAviso a
            where a.presupuesto.id in :ids
            group by a.presupuesto.id
            """)
    List<Object[]> resumenPorPresupuestoIds(@Param("ids") Collection<Long> ids);

}
