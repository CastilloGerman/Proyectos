package com.appgestion.api.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Metadatos opcionales enviados por el cliente (navegador) al iniciar sesión.
 */
public record DeviceClientInfoRequest(
        /** Nombre amistoso: p. ej. "Chrome en Windows" o modelo móvil si el usuario lo indica. */
        @Size(max = 200)
        String deviceLabel,
        /** Plataforma del sistema (p. ej. userAgentData.platform). */
        @Size(max = 120)
        String platform,
        @Size(max = 120)
        String vendor
) {}
