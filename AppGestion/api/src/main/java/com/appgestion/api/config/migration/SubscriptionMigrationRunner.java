package com.appgestion.api.config.migration;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.SubscriptionStatus;
import com.appgestion.api.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Migra usuarios existentes al nuevo modelo de suscripción.
 * Ejecuta una sola vez al arrancar la aplicación.
 */
@Component
public class SubscriptionMigrationRunner {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionMigrationRunner.class);

    private final UsuarioRepository usuarioRepository;
    private final EntityManager entityManager;

    public SubscriptionMigrationRunner(UsuarioRepository usuarioRepository, EntityManager entityManager) {
        this.usuarioRepository = usuarioRepository;
        this.entityManager = entityManager;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrateExistingUsers() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        int updated = 0;
        for (Usuario u : usuarios) {
            if (u != null && needsMigration(u)) {
                applyMigration(u);
                usuarioRepository.save(u);
                updated++;
            }
        }
        if (updated > 0) {
            log.info("SubscriptionMigrationRunner: migrados {} usuarios al nuevo modelo de suscripción", updated);
        }
    }

    private boolean needsMigration(Usuario u) {
        if (u.getSubscriptionStatus() != null && u.getTrialStartDate() != null) {
            return false;
        }
        if (u.getSubscriptionStatus() == null) {
            return true;
        }
        return u.getTrialStartDate() == null && (u.getSubscriptionStatus() == SubscriptionStatus.TRIAL_ACTIVE
                || u.getSubscriptionStatus() == SubscriptionStatus.TRIAL_EXPIRED);
    }

    @SuppressWarnings("unchecked")
    private String getRawSubscriptionStatus(Long usuarioId) {
        try {
            List<Object> rows = (List<Object>) entityManager.createNativeQuery(
                    "SELECT subscription_status FROM usuarios WHERE id = :id")
                    .setParameter("id", usuarioId)
                    .getResultList();
            if (!rows.isEmpty() && rows.get(0) != null) {
                return rows.get(0).toString();
            }
        } catch (Exception e) {
            log.debug("No se pudo leer subscription_status raw para usuario {}: {}", usuarioId, e.getMessage());
        }
        return null;
    }

    private void applyMigration(Usuario u) {
        String raw = getRawSubscriptionStatus(u.getId());
        LocalDate today = LocalDate.now();

        if (u.getSubscriptionStatus() == null) {
            if ("active".equalsIgnoreCase(raw) && u.getSubscriptionCurrentPeriodEnd() != null
                    && u.getSubscriptionCurrentPeriodEnd().isAfter(java.time.LocalDateTime.now())) {
                u.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
                return;
            }
            u.setTrialStartDate(today.minusDays(14));
            u.setTrialEndDate(today);
            u.setSubscriptionStatus(SubscriptionStatus.TRIAL_EXPIRED);
            return;
        }

        if (u.getTrialStartDate() == null && (u.getSubscriptionStatus() == SubscriptionStatus.TRIAL_ACTIVE
                || u.getSubscriptionStatus() == SubscriptionStatus.TRIAL_EXPIRED)) {
            u.setTrialStartDate(today.minusDays(14));
            u.setTrialEndDate(today);
        }
    }
}
