package com.appgestion.api.security;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Generación y verificación de códigos TOTP (compatible con Google Authenticator, Authy, etc.).
 */
@Service
public class TotpService {

    private final GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();

    @Value("${app.totp.issuer:AppGestion}")
    private String issuer;

    public String getIssuer() {
        return issuer;
    }

    public GoogleAuthenticatorKey generateKey() {
        return googleAuthenticator.createCredentials();
    }

    public String buildOtpAuthUrl(String accountEmail, GoogleAuthenticatorKey key) {
        return GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(issuer, accountEmail, key);
    }

    /**
     * @param secretBase32 secreto en Base32 (como lo devuelve la librería)
     * @param code6        código de 6 dígitos introducido por el usuario
     */
    public boolean verify(String secretBase32, String code6) {
        if (secretBase32 == null || secretBase32.isBlank() || code6 == null) {
            return false;
        }
        String normalized = code6.trim().replaceAll("\\s+", "");
        if (!normalized.matches("\\d{6}")) {
            return false;
        }
        try {
            int verificationCode = Integer.parseInt(normalized);
            return googleAuthenticator.authorize(secretBase32, verificationCode);
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
