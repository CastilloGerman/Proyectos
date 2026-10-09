package com.appgestion.api.repository;

import com.appgestion.api.domain.entity.PresupuestoEnlace;
import com.appgestion.api.repository.projection.PresupuestoEnlaceResumen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.List;

public interface PresupuestoEnlaceRepository extends JpaRepository<PresupuestoEnlace, Long> {
    Optional<PresupuestoEnlace> findByTokenHashAndRevocadoFalseAndExpiraAtAfter(String tokenHash, Instant now);
    List<PresupuestoEnlace> findAllByPresupuestoIdAndPresupuestoUsuarioId(Long presupuestoId, Long usuarioId);

    long countByPresupuestoIdAndPresupuestoUsuarioIdAndRevocadoFalseAndExpiraAtAfter(
            Long presupuestoId, Long usuarioId, Instant now);

    @Query("""
            select min(e.primeraVistaAt) as primeraVistaAt,
                   max(e.ultimaVistaAt) as ultimaVistaAt,
                   coalesce(sum(e.numVistas), 0) as numVistas
            from PresupuestoEnlace e
            where e.presupuesto.id = :presupuestoId and e.presupuesto.usuario.id = :usuarioId
            """)
    PresupuestoEnlaceResumen summarizeViews(@Param("presupuestoId") Long presupuestoId,
                                             @Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("update PresupuestoEnlace e set e.revocado = true where e.presupuesto.id = :presupuestoId and e.presupuesto.usuario.id = :usuarioId and e.revocado = false")
    int revokeAll(@Param("presupuestoId") Long presupuestoId, @Param("usuarioId") Long usuarioId);

    @Modifying
    @Query(value = "UPDATE presupuesto_enlace SET primera_vista_at = :now, ultima_vista_at = :now, num_vistas = 1 WHERE id = :id AND primera_vista_at IS NULL", nativeQuery = true)
    int markFirstView(@Param("id") Long id, @Param("now") Instant now);

    @Modifying
    @Query(value = "UPDATE presupuesto_enlace SET ultima_vista_at = :now, num_vistas = num_vistas + 1 WHERE id = :id AND primera_vista_at IS NOT NULL AND ultima_vista_at <= :cutoff", nativeQuery = true)
    int countSubsequentView(@Param("id") Long id, @Param("now") Instant now, @Param("cutoff") Instant cutoff);
}
