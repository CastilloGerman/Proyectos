package com.appgestion.api.config.migration;

import com.appgestion.api.service.crypto.EncryptedStringConverter;
import com.appgestion.api.service.crypto.TokenEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * SEC-06: cifra los secretos que estuvieran guardados en texto plano antes de introducir
 * {@link EncryptedStringConverter} (secreto TOTP y contraseña SMTP). Es idempotente: solo toca filas cuyo valor no
 * empieza por el prefijo {@code enc:v1:}, por lo que puede ejecutarse en cada arranque sin coste apreciable.
 */
@Component
public class EncryptedSecretsMigrationRunner {

    private static final Logger log = LoggerFactory.getLogger(EncryptedSecretsMigrationRunner.class);

    private final JdbcTemplate jdbc;
    private final TokenEncryptionService encryption;

    public EncryptedSecretsMigrationRunner(JdbcTemplate jdbc, TokenEncryptionService encryption) {
        this.jdbc = jdbc;
        this.encryption = encryption;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void encryptLegacyPlaintextSecrets() {
        try {
            int total = migrateColumn("usuarios", "totp_secret")
                    + migrateColumn("usuarios", "totp_pending_secret")
                    + migrateColumn("empresas", "mail_password");
            if (total > 0) {
                log.info("EncryptedSecretsMigrationRunner: {} secretos en texto plano cifrados", total);
            }
        } catch (Exception e) {
            // No tumbar el arranque: los valores legado siguen siendo legibles y se reintentará en el siguiente arranque.
            log.error("EncryptedSecretsMigrationRunner: no se pudieron cifrar los secretos heredados: {}", e.toString());
        }
    }

    /** Nombres de tabla/columna son constantes internas (nunca entrada de usuario). */
    private int migrateColumn(String table, String column) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, " + column + " AS v FROM " + table
                        + " WHERE " + column + " IS NOT NULL AND " + column + " <> '' AND " + column + " NOT LIKE ?",
                EncryptedStringConverter.PREFIX + "%");
        int updated = 0;
        for (Map<String, Object> row : rows) {
            Object id = row.get("id");
            String plain = String.valueOf(row.get("v"));
            String enc = EncryptedStringConverter.PREFIX + encryption.encrypt(plain);
            updated += jdbc.update(
                    "UPDATE " + table + " SET " + column + " = ? WHERE id = ? AND " + column + " = ?",
                    enc, id, plain);
        }
        return updated;
    }
}
