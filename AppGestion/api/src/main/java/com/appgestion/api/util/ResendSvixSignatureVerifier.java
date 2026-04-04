package com.appgestion.api.util;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Verificación de firmas de webhooks Resend (esquema <a href="https://docs.svix.com/receiving/verifying-payloads/how">Svix</a>).
 * Cuerpo en bruto (bytes UTF-8 del payload tal cual llegó) + cabeceras {@code svix-id}, {@code svix-timestamp}, {@code svix-signature}.
 */
public final class ResendSvixSignatureVerifier {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final long TIMESTAMP_TOLERANCE_SEC = 300;

    private ResendSvixSignatureVerifier() {}

    /**
     * @param signingSecret secreto del dashboard Resend; si empieza por {@code whsec_}, la parte posterior se decodifica en Base64 como clave HMAC.
     */
    public static boolean verify(
            String rawPayload,
            String svixId,
            String svixTimestamp,
            String svixSignatureHeader,
            String signingSecret) {
        if (signingSecret == null || signingSecret.isBlank()) {
            return true;
        }
        if (rawPayload == null
                || svixId == null
                || svixId.isBlank()
                || svixTimestamp == null
                || svixTimestamp.isBlank()
                || svixSignatureHeader == null
                || svixSignatureHeader.isBlank()) {
            return false;
        }
        long ts;
        try {
            ts = Long.parseLong(svixTimestamp.trim());
        } catch (NumberFormatException e) {
            return false;
        }
        long now = System.currentTimeMillis() / 1000;
        if (Math.abs(now - ts) > TIMESTAMP_TOLERANCE_SEC) {
            return false;
        }
        byte[] key = decodeSigningSecret(signingSecret.trim());
        if (key.length == 0) {
            return false;
        }
        String signedContent = svixId + "." + svixTimestamp + "." + rawPayload;
        byte[] expected;
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(key, HMAC_SHA256));
            expected = mac.doFinal(signedContent.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            return false;
        }
        for (String part : svixSignatureHeader.trim().split("\\s+")) {
            int comma = part.indexOf(',');
            if (comma <= 0 || comma >= part.length() - 1) {
                continue;
            }
            String version = part.substring(0, comma);
            String sigB64 = part.substring(comma + 1);
            if (!"v1".equals(version)) {
                continue;
            }
            try {
                byte[] sig = Base64.getDecoder().decode(sigB64);
                if (sig.length > 0 && MessageDigest.isEqual(expected, sig)) {
                    return true;
                }
            } catch (IllegalArgumentException ignored) {
                // firma base64 inválida
            }
        }
        return false;
    }

    private static byte[] decodeSigningSecret(String secret) {
        if (secret.startsWith("whsec_")) {
            try {
                return Base64.getDecoder().decode(secret.substring("whsec_".length()));
            } catch (IllegalArgumentException e) {
                return new byte[0];
            }
        }
        return secret.getBytes(StandardCharsets.UTF_8);
    }
}
