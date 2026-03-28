package com.appgestion.api.domain.enums;

/**
 * Cómo envía correo la organización ({@code empresas.email_provider}).
 */
public enum EmailProviderMode {
    system,
    gmail,
    outlook,
    smtp_legacy
}
