package com.appgestion.api.dto.request;

/**
 * Metadatos opcionales enviados por el cliente (navegador) al iniciar sesión.
 */
public record DeviceClientInfoRequest(
        /** Nombre amistoso: p. ej. "Chrome en Windows" o modelo móvil si el usuario lo indica. */
        String deviceLabel,
        /** Plataforma del sistema (p. ej. userAgentData.platform). */
        String platform,
        String vendor
) {}
