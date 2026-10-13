package com.appgestion.api.service.crypto;

import com.appgestion.api.config.AppEmailProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Cifrado AES-256-GCM para secretos en reposo.
 * <p>
 * <b>Formatos</b> (SEC-26):
 * <ul>
 *   <li>{@code v2.<base64(iv||ciphertext||tag)>}: la clave AES se deriva del secreto con HKDF-SHA256 (RFC 5869,
 *       con sal e información de contexto fijas), de modo que no se usa el hash directo del secreto y la misma
 *       clave maestra puede reutilizarse sin compartir clave de cifrado con otros usos.</li>
 *   <li>Sin prefijo: formato legado (clave = SHA-256 del secreto). Se sigue pudiendo DESCIFRAR para no perder datos
 *       existentes; al volver a guardarse el valor se cifra ya en v2.</li>
 * </ul>
 * <b>Rotación</b>: si se define {@code app.email.previous-token-encryption-key}, un valor que la clave actual no puede
 * abrir se intenta con la anterior. Basta con desplegar con ambas claves, dejar que los valores se reescriban (o
 * ejecutar una migración) y retirar la anterior.
 */
@Service
public class TokenEncryptionService {

    static final String V2_PREFIX = "v2.";

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final byte[] HKDF_SALT = "appgestion/token-encryption/v2".getBytes(StandardCharsets.UTF_8);
    private static final byte[] HKDF_INFO = "aes-256-gcm".getBytes(StandardCharsets.UTF_8);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppEmailProperties props;

    public TokenEncryptionService(AppEmailProperties props) {
        this.props = props;
    }

    public String encrypt(String plain) {
        if (plain == null || plain.isEmpty()) {
            return null;
        }
        byte[] keyBytes = hkdfKey(requireKey(props.getTokenEncryptionKey()));
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKey key = new SecretKeySpec(keyBytes, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buf = ByteBuffer.allocate(iv.length + ct.length);
            buf.put(iv);
            buf.put(ct);
            return V2_PREFIX + Base64.getEncoder().encodeToString(buf.array());
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cifrar el secreto", e);
        }
    }

    public String decrypt(String encoded) {
        if (!StringUtils.hasText(encoded)) {
            return null;
        }
        boolean v2 = encoded.startsWith(V2_PREFIX);
        String payload = v2 ? encoded.substring(V2_PREFIX.length()) : encoded;

        String current = requireKey(props.getTokenEncryptionKey());
        try {
            return decryptWith(payload, v2 ? hkdfKey(current) : legacyKey(current));
        } catch (Exception first) {
            String previous = props.getPreviousTokenEncryptionKey();
            if (StringUtils.hasText(previous) && previous.length() >= 16) {
                try {
                    return decryptWith(payload, v2 ? hkdfKey(previous) : legacyKey(previous));
                } catch (Exception ignored) {
                    // se informa con el error de la clave actual
                }
            }
            throw new IllegalStateException("No se pudo descifrar el secreto", first);
        }
    }

    private static String decryptWith(String base64Payload, byte[] keyBytes) throws Exception {
        byte[] all = Base64.getDecoder().decode(base64Payload);
        ByteBuffer buf = ByteBuffer.wrap(all);
        byte[] iv = new byte[GCM_IV_LENGTH];
        buf.get(iv);
        byte[] ct = new byte[buf.remaining()];
        buf.get(ct);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
    }

    private static String requireKey(String raw) {
        if (!StringUtils.hasText(raw) || raw.length() < 16) {
            throw new IllegalStateException(
                    "Configura app.email.token-encryption-key (mínimo 16 caracteres) para OAuth.");
        }
        return raw;
    }

    /** HKDF-SHA256 (extract + expand de un único bloque: 32 bytes). */
    static byte[] hkdfKey(String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(HKDF_SALT, "HmacSHA256"));
            byte[] prk = mac.doFinal(secret.getBytes(StandardCharsets.UTF_8));

            mac.init(new SecretKeySpec(prk, "HmacSHA256"));
            mac.update(HKDF_INFO);
            mac.update((byte) 0x01);
            return Arrays.copyOf(mac.doFinal(), 32);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Derivación anterior (SHA-256 directo); solo para leer datos legados. */
    private static byte[] legacyKey(String raw) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return Arrays.copyOf(sha.digest(raw.getBytes(StandardCharsets.UTF_8)), 32);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
