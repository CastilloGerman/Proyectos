package com.appgestion.api.domain.entity;

import com.appgestion.api.domain.enums.SubscriptionStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /** Teléfono de contacto (perfil); opcional. */
    @Column(length = 30)
    private String telefono;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(length = 50)
    private String rol = "USER";

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "stripe_customer_id", length = 100)
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id", length = 100)
    private String stripeSubscriptionId;

    @Convert(converter = SubscriptionStatusConverter.class)
    @Column(name = "subscription_status", length = 30)
    private SubscriptionStatus subscriptionStatus;

    @Column(name = "subscription_current_period_end")
    private LocalDateTime subscriptionCurrentPeriodEnd;

    @Column(name = "trial_start_date")
    private LocalDate trialStartDate;

    @Column(name = "trial_end_date")
    private LocalDate trialEndDate;

    @Column(name = "password_reset_token", length = 255)
    private String passwordResetToken;

    @Column(name = "password_reset_expires_at")
    private LocalDateTime passwordResetExpiresAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /** Usuario que envió el enlace de referido (si el alta fue por invitación). */
    @Column(name = "referred_by_usuario_id")
    private Long referredByUsuarioId;

    /** Avisos de facturación, suscripción y pagos por email. */
    @Column(name = "email_notify_billing", nullable = false)
    private boolean emailNotifyBilling = true;

    /** Recordatorios y avisos sobre presupuestos/facturas por email. */
    @Column(name = "email_notify_documents", nullable = false)
    private boolean emailNotifyDocuments = true;

    /** Novedades y consejos (comunicación comercial ligera). */
    @Column(name = "email_notify_marketing", nullable = false)
    private boolean emailNotifyMarketing = false;

    /** Código de idioma de interfaz (ej. es, en). */
    @Column(name = "ui_locale", nullable = false, length = 10)
    private String uiLocale = "es";

    /** Zona horaria IANA (ej. Europe/Madrid). */
    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone = "Europe/Madrid";

    /** Código ISO 4217 de moneda preferida para importes en UI. */
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "EUR";

    /** Fecha de nacimiento (perfil); opcional. */
    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    /** Género autodeclarado: MALE, FEMALE, NON_BINARY, OTHER, UNSPECIFIED; opcional. */
    @Column(length = 32)
    private String genero;

    /** Nacionalidad ISO 3166-1 alpha-2; opcional. */
    @Column(name = "nacionalidad_iso", length = 2)
    private String nacionalidadIso;

    /** País de residencia ISO 3166-1 alpha-2; opcional. */
    @Column(name = "pais_residencia_iso", length = 2)
    private String paisResidenciaIso;

    /** Secreto TOTP activo (Base32). Cifrado en reposo sería mejora futura. */
    @Column(name = "totp_secret", length = 64)
    private String totpSecret;

    @Column(name = "totp_enabled", nullable = false)
    private Boolean totpEnabled = false;

    /** Secreto temporal mientras el usuario confirma el enrolamiento. */
    @Column(name = "totp_pending_secret", length = 64)
    private String totpPendingSecret;

    @Column(name = "totp_pending_expires_at")
    private LocalDateTime totpPendingExpiresAt;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
    }

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getStripeCustomerId() { return stripeCustomerId; }
    public void setStripeCustomerId(String stripeCustomerId) { this.stripeCustomerId = stripeCustomerId; }

    public String getStripeSubscriptionId() { return stripeSubscriptionId; }
    public void setStripeSubscriptionId(String stripeSubscriptionId) { this.stripeSubscriptionId = stripeSubscriptionId; }

    public SubscriptionStatus getSubscriptionStatus() { return subscriptionStatus; }
    public void setSubscriptionStatus(SubscriptionStatus subscriptionStatus) { this.subscriptionStatus = subscriptionStatus; }

    public LocalDateTime getSubscriptionCurrentPeriodEnd() { return subscriptionCurrentPeriodEnd; }
    public void setSubscriptionCurrentPeriodEnd(LocalDateTime subscriptionCurrentPeriodEnd) { this.subscriptionCurrentPeriodEnd = subscriptionCurrentPeriodEnd; }

    public LocalDate getTrialStartDate() { return trialStartDate; }
    public void setTrialStartDate(LocalDate trialStartDate) { this.trialStartDate = trialStartDate; }

    public LocalDate getTrialEndDate() { return trialEndDate; }
    public void setTrialEndDate(LocalDate trialEndDate) { this.trialEndDate = trialEndDate; }

    public String getPasswordResetToken() { return passwordResetToken; }
    public void setPasswordResetToken(String passwordResetToken) { this.passwordResetToken = passwordResetToken; }

    public LocalDateTime getPasswordResetExpiresAt() { return passwordResetExpiresAt; }
    public void setPasswordResetExpiresAt(LocalDateTime passwordResetExpiresAt) { this.passwordResetExpiresAt = passwordResetExpiresAt; }

    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization organization) { this.organization = organization; }

    public Long getReferredByUsuarioId() { return referredByUsuarioId; }
    public void setReferredByUsuarioId(Long referredByUsuarioId) { this.referredByUsuarioId = referredByUsuarioId; }

    public boolean isEmailNotifyBilling() { return emailNotifyBilling; }
    public void setEmailNotifyBilling(boolean emailNotifyBilling) { this.emailNotifyBilling = emailNotifyBilling; }

    public boolean isEmailNotifyDocuments() { return emailNotifyDocuments; }
    public void setEmailNotifyDocuments(boolean emailNotifyDocuments) { this.emailNotifyDocuments = emailNotifyDocuments; }

    public boolean isEmailNotifyMarketing() { return emailNotifyMarketing; }
    public void setEmailNotifyMarketing(boolean emailNotifyMarketing) { this.emailNotifyMarketing = emailNotifyMarketing; }

    public String getUiLocale() { return uiLocale; }
    public void setUiLocale(String uiLocale) { this.uiLocale = uiLocale; }

    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }

    public String getNacionalidadIso() { return nacionalidadIso; }
    public void setNacionalidadIso(String nacionalidadIso) { this.nacionalidadIso = nacionalidadIso; }

    public String getPaisResidenciaIso() { return paisResidenciaIso; }
    public void setPaisResidenciaIso(String paisResidenciaIso) { this.paisResidenciaIso = paisResidenciaIso; }

    public String getTotpSecret() { return totpSecret; }
    public void setTotpSecret(String totpSecret) { this.totpSecret = totpSecret; }

    public Boolean getTotpEnabled() { return totpEnabled; }
    public void setTotpEnabled(Boolean totpEnabled) { this.totpEnabled = totpEnabled; }

    public String getTotpPendingSecret() { return totpPendingSecret; }
    public void setTotpPendingSecret(String totpPendingSecret) { this.totpPendingSecret = totpPendingSecret; }

    public LocalDateTime getTotpPendingExpiresAt() { return totpPendingExpiresAt; }
    public void setTotpPendingExpiresAt(LocalDateTime totpPendingExpiresAt) { this.totpPendingExpiresAt = totpPendingExpiresAt; }
}
