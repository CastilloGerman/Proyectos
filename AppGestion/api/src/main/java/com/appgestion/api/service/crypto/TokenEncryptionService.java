package com.appgestion.api.service.crypto;

import com.appgestion.api.config.AppEmailProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
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
 * Cifrado AES-256-GCM para tokens en reposo.
 */
@Service
public class TokenEncryptionService {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final AppEmailProperties props;

    public TokenEncryptionService(AppEmailProperties props) {
        this.props = props;
    }

    public String encrypt(String plain) {
        if (plain == null || plain.isEmpty()) {
            return null;
        }
        byte[] keyBytes = deriveKey();
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKey key = new SecretKeySpec(keyBytes, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buf = ByteBuffer.allocate(iv.length + ct.length);
            buf.put(iv);
            buf.put(ct);
            return Base64.getEncoder().encodeToString(buf.array());
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cifrar el secreto", e);
        }
    }

    public String decrypt(String encoded) {
        if (!StringUtils.hasText(encoded)) {
            return null;
        }
        byte[] keyBytes = deriveKey();
        try {
            byte[] all = Base64.getDecoder().decode(encoded);
            ByteBuffer buf = ByteBuffer.wrap(all);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buf.get(iv);
            byte[] ct = new byte[buf.remaining()];
            buf.get(ct);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKey key = new SecretKeySpec(keyBytes, "AES");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] pt = cipher.doFinal(ct);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo descifrar el secreto", e);
        }
    }

    private byte[] deriveKey() {
        String raw = props.getTokenEncryptionKey();
        if (!StringUtils.hasText(raw) || raw.length() < 16) {
            throw new IllegalStateException(
                    "Configura app.email.token-encryption-key (mínimo 16 caracteres) para OAuth.");
        }
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] digest = sha.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Arrays.copyOf(digest, 32);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
