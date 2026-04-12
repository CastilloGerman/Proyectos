package com.appgestion.api.unit.util;

import com.appgestion.api.util.ResendSvixSignatureVerifier;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class ResendSvixSignatureVerifierTest {

    @Test
    void verify_acceptsValidV1Signature() throws Exception {
        byte[] keyBytes = "test_secret_key_16".getBytes(StandardCharsets.UTF_8);
        String secret = "whsec_" + Base64.getEncoder().encodeToString(keyBytes);
        String payload = "{\"type\":\"email.sent\"}";
        String id = "msg_test_1";
        String ts = String.valueOf(Instant.now().getEpochSecond());
        String signedContent = id + "." + ts + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
        String sigB64 = Base64.getEncoder().encodeToString(mac.doFinal(signedContent.getBytes(StandardCharsets.UTF_8)));
        String header = "v1," + sigB64;

        assertThat(ResendSvixSignatureVerifier.verify(payload, id, ts, header, secret)).isTrue();
    }

    @Test
    void verify_rejectsWrongSignature() {
        byte[] keyBytes = "test_secret_key_16".getBytes(StandardCharsets.UTF_8);
        String secret = "whsec_" + Base64.getEncoder().encodeToString(keyBytes);
        String payload = "{}";
        String id = "msg_x";
        String ts = String.valueOf(Instant.now().getEpochSecond());
        assertThat(ResendSvixSignatureVerifier.verify(payload, id, ts, "v1," + Base64.getEncoder().encodeToString(new byte[] {1, 2, 3}), secret))
                .isFalse();
    }

    @Test
    void verify_blankSecret_skipsCheck() {
        assertThat(ResendSvixSignatureVerifier.verify("{}", null, null, null, "  ")).isTrue();
    }
}
