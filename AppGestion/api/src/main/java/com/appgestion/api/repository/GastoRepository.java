package com.appgestion.api.repository;

import com.appgestion.api.domain.entity.Gasto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GastoRepository extends JpaRepository<Gasto, Long> {

    List<Gasto> findByUsuarioIdOrderByFechaDesc(Long usuarioId);

    List<Gasto> findByUsuarioIdAndPresupuestoIdOrderByFechaDesc(Long usuarioId, Long presupuestoId);

    @Query("""
            select g from Gasto g
            left join fetch g.presupuesto p
            left join fetch p.cliente
            where g.usuario.id = :usuarioId
            order by g.fecha desc, g.id desc
            """)
    List<Gasto> findByUsuarioIdForList(@Param("usuarioId") Long usuarioId);

    @Query("""
            select g from Gasto g
            left join fetch g.presupuesto p
            left join fetch p.cliente
            where g.usuario.id = :usuarioId and g.presupuesto.id = :presupuestoId
            order by g.fecha desc, g.id desc
            """)
    List<Gasto> findByUsuarioIdAndPresupuestoIdForList(@Param("usuarioId") Long usuarioId,
                                                       @Param("presupuestoId") Long presupuestoId);

    Optional<Gasto> findByIdAndUsuarioId(Long id, Long usuarioId);

    boolean existsByIdAndUsuarioId(Long id, Long usuarioId);
}
