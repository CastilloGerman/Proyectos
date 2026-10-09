package com.appgestion.api.repository;

import com.appgestion.api.domain.enums.SubscriptionStatus;
import com.appgestion.api.domain.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    Optional<Usuario> findByStripeSubscriptionId(String stripeSubscriptionId);

    Optional<Usuario> findByStripeCustomerId(String stripeCustomerId);

    @Query("SELECT u FROM Usuario u WHERE u.subscriptionStatus = :status AND u.trialEndDate < :today")
    List<Usuario> findExpiredTrials(@Param("status") SubscriptionStatus status, @Param("today") LocalDate today);

    Optional<Usuario> findByPasswordResetToken(String token);

    @Query("""
            select distinct u.id from Usuario u
            where u.id > :lastId
              and exists (
                  select p.id from Presupuesto p
                  where p.usuario.id = u.id
                    and p.enviadoAt is not null
                    and p.seguimientoSilenciado = false
              )
            order by u.id
            """)
    Page<Long> findFollowupOwnerIdsAfter(@Param("lastId") Long lastId, Pageable pageable);
}
