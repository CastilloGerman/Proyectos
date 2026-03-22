package com.appgestion.api.repository;

import com.appgestion.api.domain.entity.UsuarioSesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UsuarioSesionRepository extends JpaRepository<UsuarioSesion, String> {

    @Query("SELECT s FROM UsuarioSesion s JOIN FETCH s.usuario WHERE s.id = :id")
    Optional<UsuarioSesion> findByIdWithUsuario(@Param("id") String id);

    List<UsuarioSesion> findByUsuarioIdOrderByLastActivityAtDesc(Long usuarioId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE UsuarioSesion s SET s.revokedAt = :now WHERE s.usuario.id = :uid AND s.id <> :keepId AND s.revokedAt IS NULL")
    int revokeAllForUserExcept(@Param("uid") Long usuarioId, @Param("keepId") String keepSessionId, @Param("now") Instant now);

    /** Filas cuyo JWT ya no puede ser válido (expiración anterior al corte). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM UsuarioSesion s WHERE s.expiresAt < :before")
    int deleteByExpiresAtBefore(@Param("before") Instant before);
}
