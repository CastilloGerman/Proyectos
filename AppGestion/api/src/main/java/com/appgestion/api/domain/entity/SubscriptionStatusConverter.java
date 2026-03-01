package com.appgestion.api.domain.entity;

import com.appgestion.api.domain.enums.SubscriptionStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Convierte entre SubscriptionStatus enum y valores en BD.
 * Acepta tanto valores legacy (active, trialing, past_due, canceled)
 * como los nuevos (ACTIVE, TRIAL_ACTIVE, etc.) para migración gradual.
 */
@Converter(autoApply = false)
public class SubscriptionStatusConverter implements AttributeConverter<SubscriptionStatus, String> {

    @Override
    public String convertToDatabaseColumn(SubscriptionStatus status) {
        return status == null ? null : status.name();
    }

    @Override
    public SubscriptionStatus convertToEntityAttribute(String dbValue) {
        if (dbValue == null || dbValue.isBlank()) {
            return null;
        }
        String normalized = dbValue.trim().toUpperCase();
        return switch (normalized) {
            case "ACTIVE" -> SubscriptionStatus.ACTIVE;
            case "TRIAL_ACTIVE" -> SubscriptionStatus.TRIAL_ACTIVE;
            case "TRIAL_EXPIRED" -> SubscriptionStatus.TRIAL_EXPIRED;
            case "PAST_DUE" -> SubscriptionStatus.PAST_DUE;
            case "CANCELED" -> SubscriptionStatus.CANCELED;
            case "CANCELLED" -> SubscriptionStatus.CANCELED;
            case "TRIALING" -> SubscriptionStatus.TRIAL_ACTIVE;
            case "PASTDUE" -> SubscriptionStatus.PAST_DUE;
            case "UNPAID", "INCOMPLETE_EXPIRED" -> SubscriptionStatus.CANCELED;
            default -> SubscriptionStatus.TRIAL_EXPIRED;
        };
    }
}
