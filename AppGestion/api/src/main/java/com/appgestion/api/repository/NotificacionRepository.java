package com.appgestion.api.repository;

import com.appgestion.api.domain.entity.Notificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    long countByUsuarioIdAndLeidaIsFalse(Long usuarioId);

    long countByUsuarioId(Long usuarioId);

    @Query("SELECT n FROM Notificacion n WHERE n.usuario.id = :uid AND (:read IS NULL OR n.leida = :read)")
    Page<Notificacion> findForUsuario(
            @Param("uid") Long usuarioId,
            @Param("read") Boolean readFilter,
            Pageable pageable
    );

    /** {@code usuario.id} en Spring Data → {@code Usuario_Id} */
    Optional<Notificacion> findByIdAndUsuario_Id(Long id, Long usuarioId);

    @Modifying
    @Query("UPDATE Notificacion n SET n.leida = true WHERE n.usuario.id = :uid AND n.leida = false")
    int markAllReadForUsuario(@Param("uid") Long usuarioId);
}
