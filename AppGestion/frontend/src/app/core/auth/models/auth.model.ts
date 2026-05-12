/** Opcional: se envía en login/registro/Google/invitación para la tabla de sesiones. */
export interface DeviceClientInfo {
  deviceLabel: string;
  platform?: string;
  vendor?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
  /** Código TOTP de 6 dígitos cuando el backend responde TOTP_REQUERIDO. */
  totpCode?: string;
  clientInfo?: DeviceClientInfo;
}

export interface RegisterRequest {
  nombre: string;
  email: string;
  password: string;
  rol?: string;
  /** Token de referido (?ref=) verificado antes del POST /auth/register. */
  referralToken?: string;
  clientInfo?: DeviceClientInfo;
}

export interface InviteVerifyResponse {
  valid: boolean;
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
  /** Id de fila `usuario_sesion` (JWT claim sid). */
  sessionId?: string;
}

export interface SesionDispositivoDto {
  id: string;
  createdAt: string;
  lastActivityAt: string;
  expiresAt: string;
  ipAddress?: string | null;
  browser?: string | null;
  osName?: string | null;
  deviceType?: string | null;
  clientLabel?: string | null;
  userAgentPreview?: string | null;
  currentSession: boolean;
}

export interface TotpSetupStartResponse {
  otpAuthUrl: string;
  secretBase32: string;
  pendingExpiresInMinutes: number;
}
