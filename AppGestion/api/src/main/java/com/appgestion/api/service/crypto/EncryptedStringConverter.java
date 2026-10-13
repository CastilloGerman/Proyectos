package com.appgestion.api.service.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * Cifra en reposo (AES-256-GCM, vía {@link TokenEncryptionService}) los secretos guardados en columnas de texto:
 * secreto TOTP, contraseña SMTP, etc.
 * <p>
 * Formato en BD: {@code enc:v1:<base64(iv||ciphertext||tag)>}. Los valores SIN ese prefijo se consideran legado
 * (texto plano previo a SEC-06) y se devuelven tal cual al leer; {@code EncryptedSecretsMigrationRunner} los cifra
 * al arrancar.
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    public static final String PREFIX = "enc:v1:";

    private final TokenEncryptionService encryption;

    public EncryptedStringConverter(TokenEncryptionService encryption) {
        this.encryption = encryption;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }
        return PREFIX + encryption.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }
        if (!dbData.startsWith(PREFIX)) {
            return dbData; // legado en texto plano
        }
        return encryption.decrypt(dbData.substring(PREFIX.length()));
    }

    public static boolean isEncrypted(String dbValue) {
        return dbValue != null && dbValue.startsWith(PREFIX);
    }
}
