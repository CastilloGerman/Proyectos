package com.appgestion.api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record UsuarioResponse(
        Long id,
        String nombre,
        String email,
        String telefono,
        String rol,
        Boolean activo,
        LocalDateTime fechaCreacion,
        String subscriptionStatus,
        LocalDate trialEndDate,
        /** Current Stripe billing period end, when applicable. */
        LocalDateTime subscriptionCurrentPeriodEnd,
        /** True when the user has a Stripe customer and can open the billing portal. */
        boolean billingPortalAvailable,
        String locale,
        String timeZone,
        String currencyCode,
        boolean emailNotifyBilling,
        boolean emailNotifyDocuments,
        boolean emailNotifyMarketing,
        boolean canWrite,
        boolean totpEnabled,
        /** True when a TOTP enrollment has been started but not confirmed yet. */
        boolean totpEnrollmentPending,
        LocalDate fechaNacimiento,
        String genero,
        String nacionalidadIso,
        String paisResidenciaIso,
        /** Default budget condition keys selected from the server catalog. */
        List<String> condicionesPresupuestoPredeterminadas
) {}
