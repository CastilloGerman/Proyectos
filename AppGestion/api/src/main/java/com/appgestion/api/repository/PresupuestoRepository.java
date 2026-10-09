package com.appgestion.api.repository;

import com.appgestion.api.domain.entity.Presupuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface PresupuestoRepository extends JpaRepository<Presupuesto, Long> {

    List<Presupuesto> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    List<Presupuesto> findByUsuarioIdAndClienteIdOrderByFechaCreacionDesc(Long usuarioId, Long clienteId);

    Optional<Presupuesto> findByIdAndUsuarioId(Long id, Long usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Presupuesto p where p.id = :id and p.usuario.id = :usuarioId")
    Optional<Presupuesto> findOwnedForUpdate(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("update Presupuesto p set p.enlaceVistoNotificado = true where p.id = :id and p.enlaceVistoNotificado = false")
    int markLinkViewNotificationSent(@Param("id") Long id);

    boolean existsByIdAndUsuarioId(Long id, Long usuarioId);
}
