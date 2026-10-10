package com.appgestion.api.config.migration;

import com.appgestion.api.domain.entity.AppMigrationState;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.SubscriptionStatus;
import com.appgestion.api.repository.AppMigrationStateRepository;
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
import java.util.Optional;

/**
 * Migra usuarios existentes al nuevo modelo de suscripción.
 * Ejecuta una sola vez al arrancar la aplicación, controlado por un flag en {@code app_migration_state}.
 * Si la fila con key {@code subscription_migration_v1} existe, se omite la migración (arranques en frío posteriores).
 */
@Component
public class SubscriptionMigrationRunner {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionMigrationRunner.class);
    private static final String MIGRATION_KEY = "subscription_migration_v1";

    private final UsuarioRepository usuarioRepository;
    private final AppMigrationStateRepository appMigrationStateRepository;
    private final EntityManager entityManager;

    public SubscriptionMigrationRunner(UsuarioRepository usuarioRepository,
                                       AppMigrationStateRepository appMigrationStateRepository,
                                       EntityManager entityManager) {
        this.usuarioRepository = usuarioRepository;
        this.appMigrationStateRepository = appMigrationStateRepository;
        this.entityManager = entityManager;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrateExistingUsers() {
        // Guard: si ya se ejecutó, saltar (evita cargar todos los usuarios en cada arranque en frío).
        Optional<AppMigrationState> existing;
        try {
            existing = appMigrationStateRepository.findByKey(MIGRATION_KEY);
        } catch (Exception e) {
            // Si la tabla app_migration_state no existe, es un error de despliegue.
            throw new IllegalStateException(
                    "SubscriptionMigrationRunner: la tabla 'app_migration_state' no existe. "
                            + "Ejecuta las migraciones de Flyway antes del primer arranque. "
                            + "Procedimiento: arranca el JAR con SPRING_PROFILES_ACTIVE=prod (sin perfil serverless) "
                            + "y las variables de conexión a la BD, o ejecuta 'mvn flyway:migrate' con la misma configuración. "
                            + "Detalle: " + e.getMessage(),
                    e);
        }
        if (existing.isPresent()) {
            log.debug("SubscriptionMigrationRunner: migración ya ejecutada ({}), omitiendo", MIGRATION_KEY);
            return;
        }

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
        // Marcar como ejecutada para los siguientes arranques.
        appMigrationStateRepository.save(new AppMigrationState(MIGRATION_KEY, String.valueOf(updated)));
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
