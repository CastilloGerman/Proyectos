package com.appgestion.api.repository;

import com.appgestion.api.domain.enums.SubscriptionStatus;
import com.appgestion.api.domain.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<Usuario> findByStripeSubscriptionId(String stripeSubscriptionId);

    @Query("SELECT u FROM Usuario u WHERE u.subscriptionStatus = :status AND u.trialEndDate < :today")
    List<Usuario> findExpiredTrials(@Param("status") SubscriptionStatus status, @Param("today") LocalDate today);
}
