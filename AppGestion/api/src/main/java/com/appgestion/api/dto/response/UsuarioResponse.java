package com.appgestion.api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
        /** Fin del periodo de facturación actual (Stripe), si aplica. */
        LocalDateTime subscriptionCurrentPeriodEnd,
        /** True si el usuario tiene cliente Stripe y puede abrir el portal de facturación. */
        boolean billingPortalAvailable,
        String locale,
        String timeZone,
        String currencyCode,
        boolean emailNotifyBilling,
        boolean emailNotifyDocuments,
        boolean emailNotifyMarketing,
        boolean canWrite,
        boolean totpEnabled,
        /** True si hay un enrolamiento TOTP pendiente (código de confirmación no enviado aún). */
        boolean totpEnrollmentPending
) {}
