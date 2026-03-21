package com.appgestion.api.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Preferencias de cuenta relacionadas con correo electrónico.
 * Cada campo debe enviarse explícitamente (true/false).
 */
public record UpdateAccountSettingsRequest(
        @NotNull(message = "Indica si deseas avisos de facturación")
        Boolean emailNotifyBilling,

        @NotNull(message = "Indica si deseas avisos sobre documentos")
        Boolean emailNotifyDocuments,

        @NotNull(message = "Indica si deseas novedades y consejos")
        Boolean emailNotifyMarketing
) {}
