package com.appgestion.api.repository;

import com.appgestion.api.domain.entity.Presupuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface PresupuestoRepository extends JpaRepository<Presupuesto, Long> {

    List<Presupuesto> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    List<Presupuesto> findByUsuarioIdAndClienteIdOrderByFechaCreacionDesc(Long usuarioId, Long clienteId);

    Optional<Presupuesto> findByIdAndUsuarioId(Long id, Long usuarioId);

    List<Presupuesto> findByUsuarioIdAndEnviadoAtIsNotNullAndSeguimientoSilenciadoFalseAndIdGreaterThanOrderByIdAsc(
            Long usuarioId, Long lastId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Presupuesto p where p.id = :id and p.usuario.id = :usuarioId")
    Optional<Presupuesto> findOwnedForUpdate(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("update Presupuesto p set p.enlaceVistoNotificado = true where p.id = :id and p.enlaceVistoNotificado = false")
    int markLinkViewNotificationSent(@Param("id") Long id);

    /**
     * Actualización condicional atómica para la respuesta del cliente.
     * Compara opción Y mensaje: cualquier diferencia es una actualización.
     * Incluye los valores anteriores leídos en la condición WHERE para garantizar
     * que solo una transacción puede escribir, incluso cuando H2 no reproduce
     * SELECT FOR UPDATE con fidelidad. Devuelve 1 si se actualizó, 0 si no.
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query("update Presupuesto p set p.respuestaCliente = :opcion, p.respuestaClienteAt = :at, p.respuestaClienteMensaje = :mensaje " +
            "where p.id = :id and p.usuario.id = :usuarioId " +
            "and ((p.respuestaCliente IS DISTINCT FROM :opcion) " +
            "or (p.respuestaClienteMensaje IS DISTINCT FROM :mensaje)) " +
            "and (p.respuestaCliente = :previousOption or (p.respuestaCliente IS NULL and :previousOption IS NULL)) " +
            "and (p.respuestaClienteMensaje = :previousMessage or (p.respuestaClienteMensaje IS NULL and :previousMessage IS NULL))")
    int updateRespuestaClienteIfDistinct(@Param("id") Long id, @Param("usuarioId") Long usuarioId,
                                         @Param("opcion") String opcion, @Param("at") java.time.Instant at,
                                         @Param("mensaje") String mensaje, @Param("previousOption") String previousOption,
                                         @Param("previousMessage") String previousMessage);

    boolean existsByIdAndUsuarioId(Long id, Long usuarioId);
}
