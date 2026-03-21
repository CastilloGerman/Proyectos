export interface LoginRequest {
  email: string;
  password: string;
  /** Código TOTP de 6 dígitos cuando el backend responde TOTP_REQUERIDO. */
  totpCode?: string;
}

export interface RegisterRequest {
  nombre: string;
  email: string;
  password: string;
  rol?: string;
}

export interface InviteVerifyResponse {
  valid: boolean;
  email?: string;
}

export interface AuthResponse {
  token: string;
  type: string;
  email: string;
  rol: string;
  expiresAt: string;
  /** Rellenado desde GET /auth/me o PATCH /auth/profile. */
  nombre?: string;
  /** Teléfono de contacto del perfil (opcional). */
  telefono?: string | null;
  /** Preferencias de correo (GET /auth/me, PATCH /auth/account-settings). */
  emailNotifyBilling?: boolean;
  emailNotifyDocuments?: boolean;
  emailNotifyMarketing?: boolean;
  /** Preferencias regionales (GET /auth/me, PATCH /auth/preferences). */
  locale?: string;
  timeZone?: string;
  currencyCode?: string;
  subscriptionStatus?: string;
  trialEndDate?: string;
  subscriptionCurrentPeriodEnd?: string | null;
  billingPortalAvailable?: boolean;
  canWrite?: boolean;
  /** Rellenado desde GET /auth/me. */
  totpEnabled?: boolean;
}

export interface TotpSetupStartResponse {
  otpAuthUrl: string;
  secretBase32: string;
  pendingExpiresInMinutes: number;
}
