package com.appgestion.api.unit.service;

import com.appgestion.api.config.AppEmailProperties;
import com.appgestion.api.service.crypto.TokenEncryptionService;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** SEC-26: HKDF + prefijo de versión, lectura del formato legado y rotación de clave. */
class TokenEncryptionServiceTest {

    private static final String KEY_A = "clave-maestra-de-prueba-A-0123456789";
    private static final String KEY_B = "clave-maestra-de-prueba-B-9876543210";

    private static TokenEncryptionService service(String key, String previous) {
        AppEmailProperties p = new AppEmailProperties();
        p.setTokenEncryptionKey(key);
        p.setPreviousTokenEncryptionKey(previous);
        return new TokenEncryptionService(p);
    }

    /** Cifrado del formato anterior a SEC-26 (clave = SHA-256 directo del secreto, sin prefijo). */
    private static String cifradoLegado(String plain, String secret) throws Exception {
        byte[] key = Arrays.copyOf(MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8)), 32);
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        byte[] ct = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
        ByteBuffer buf = ByteBuffer.allocate(iv.length + ct.length);
        buf.put(iv).put(ct);
        return Base64.getEncoder().encodeToString(buf.array());
    }

    @Test
    void roundTrip_usaFormatoV2_yNoRepiteElCifrado() {
        TokenEncryptionService s = service(KEY_A, "");
        String c1 = s.encrypt("secreto-totp");
        String c2 = s.encrypt("secreto-totp");

        assertTrue(c1.startsWith("v2."));
        assertNotEquals(c1, c2, "IV aleatorio: dos cifrados del mismo valor no deben coincidir");
        assertEquals("secreto-totp", s.decrypt(c1));
    }

    @Test
    void valorLegado_sigueDescifrandose() throws Exception {
        String legado = cifradoLegado("token-oauth", KEY_A);
        assertEquals("token-oauth", service(KEY_A, "").decrypt(legado));
    }

    @Test
    void claveIncorrecta_noDescifra() {
        String c = service(KEY_A, "").encrypt("x");
        assertThrows(IllegalStateException.class, () -> service(KEY_B, "").decrypt(c));
    }

    @Test
    void rotacion_laClaveAnteriorAbreValoresViejos_peroLosNuevosSeCifranConLaActual() throws Exception {
        String viejoV2 = service(KEY_A, "").encrypt("dato");
        String viejoLegado = cifradoLegado("dato2", KEY_A);

        TokenEncryptionService rotado = service(KEY_B, KEY_A);
        assertEquals("dato", rotado.decrypt(viejoV2));
        assertEquals("dato2", rotado.decrypt(viejoLegado));

        String nuevo = rotado.encrypt("dato");
        assertEquals("dato", service(KEY_B, "").decrypt(nuevo)); // ya no depende de la clave anterior
    }

    @Test
    void claveCorta_seRechaza() {
        assertThrows(IllegalStateException.class, () -> service("corta", "").encrypt("x"));
    }
}
