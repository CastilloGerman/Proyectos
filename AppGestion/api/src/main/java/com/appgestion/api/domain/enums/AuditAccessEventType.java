package com.appgestion.api.domain.enums;

/**
 * Tipos normalizados de evento para el historial de accesos / auditoría.
 * El detalle fino va en {@code failureReason} o {@code metadataJson}.
 */
public enum AuditAccessEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGOUT,
    REGISTER_SUCCESS,
    INVITE_ACCEPT_SUCCESS,
    PASSWORD_CHANGE_SUCCESS,
    PASSWORD_CHANGE_FAILURE,
    PASSWORD_RESET_REQUESTED,
    PASSWORD_RESET_COMPLETED,
    SESSION_REVOKED_DEVICE,
    SESSION_REVOKED_OTHERS,
    TOTP_ENABLED,
    TOTP_DISABLED,
    TOTP_SETUP_CANCELLED,
    AUDIT_EXPORT_JSON,
    AUDIT_EXPORT_CSV
}
