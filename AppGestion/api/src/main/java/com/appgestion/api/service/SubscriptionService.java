package com.appgestion.api.service;

import com.appgestion.api.domain.enums.SubscriptionStatus;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final UsuarioRepository usuarioRepository;

    public SubscriptionService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Expira los trials vencidos. Invocado por TrialExpirationJob.
     */
    @Transactional
    public void expireTrials() {
        LocalDate today = LocalDate.now();
        List<Usuario> expired = usuarioRepository.findExpiredTrials(SubscriptionStatus.TRIAL_ACTIVE, today);
        for (Usuario u : expired) {
            u.setSubscriptionStatus(SubscriptionStatus.TRIAL_EXPIRED);
            usuarioRepository.save(u);
        }
        if (!expired.isEmpty()) {
            log.info("SubscriptionService: expirados {} trials", expired.size());
        }
    }

    /**
     * Verifica si el usuario puede escribir (crear, editar, eliminar).
     * Solo TRIAL_ACTIVE y ACTIVE permiten escritura.
     */
    public boolean canWrite(Usuario usuario) {
        if (usuario == null) return false;
        SubscriptionStatus status = usuario.getSubscriptionStatus();
        if (status == null) return false;
        return status == SubscriptionStatus.TRIAL_ACTIVE || status == SubscriptionStatus.ACTIVE;
    }

    /**
     * Si el trial ha expirado, actualiza el estado a TRIAL_EXPIRED.
     * Debe llamarse en login para mantener consistencia.
     */
    @Transactional
    public void checkAndUpdateTrialStatus(Usuario usuario) {
        if (usuario == null) return;
        if (usuario.getSubscriptionStatus() != SubscriptionStatus.TRIAL_ACTIVE) return;
        LocalDate trialEnd = usuario.getTrialEndDate();
        if (trialEnd == null) return;
        if (LocalDate.now().isAfter(trialEnd)) {
            usuario.setSubscriptionStatus(SubscriptionStatus.TRIAL_EXPIRED);
            usuarioRepository.save(usuario);
        }
    }

    /**
     * Compatibilidad: equivalente a canWrite para el filtro de suscripción.
     */
    public boolean hasActiveSubscription(Usuario usuario) {
        return canWrite(usuario);
    }

    @Transactional
    public void activateSubscription(Long usuarioId, String stripeCustomerId, String stripeSubscriptionId,
                                     String stripeStatus, Instant currentPeriodEnd) {
        Usuario usuario = usuarioRepository.findById(Objects.requireNonNull(usuarioId))
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + usuarioId));
        usuario.setStripeCustomerId(stripeCustomerId);
        usuario.setStripeSubscriptionId(stripeSubscriptionId);
        usuario.setSubscriptionStatus(mapStripeStatusToEnum(stripeStatus));
        usuario.setSubscriptionCurrentPeriodEnd(toLocalDateTime(currentPeriodEnd));
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void updateSubscription(String stripeSubscriptionId, String stripeStatus, Instant currentPeriodEnd) {
        Usuario usuario = usuarioRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .orElse(null);
        if (usuario == null) return;
        usuario.setSubscriptionStatus(mapStripeStatusToEnum(stripeStatus));
        if (currentPeriodEnd != null) {
            usuario.setSubscriptionCurrentPeriodEnd(toLocalDateTime(currentPeriodEnd));
        }
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void cancelSubscription(String stripeSubscriptionId) {
        Usuario usuario = usuarioRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .orElse(null);
        if (usuario == null) return;
        usuario.setSubscriptionStatus(SubscriptionStatus.CANCELED);
        usuario.setStripeSubscriptionId(null);
        usuarioRepository.save(usuario);
    }

    private static SubscriptionStatus mapStripeStatusToEnum(String stripeStatus) {
        if (stripeStatus == null || stripeStatus.isBlank()) return SubscriptionStatus.TRIAL_EXPIRED;
        return switch (stripeStatus.toLowerCase()) {
            case "active" -> SubscriptionStatus.ACTIVE;
            case "past_due" -> SubscriptionStatus.PAST_DUE;
            case "canceled", "cancelled", "unpaid", "incomplete_expired" -> SubscriptionStatus.CANCELED;
            case "trialing" -> SubscriptionStatus.TRIAL_ACTIVE;
            default -> SubscriptionStatus.CANCELED;
        };
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
