import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, of, finalize } from 'rxjs';
import {
  AuthResponse,
  InviteVerifyResponse,
  LoginRequest,
  RegisterRequest,
  SesionDispositivoDto,
  TotpSetupStartResponse,
} from './models/auth.model';
import { buildDeviceClientInfo } from './device-client-info';
import { readJwtSessionId } from './jwt-sid.util';
import { environment } from '../../../environments/environment';

const TOKEN_KEY = 'appgestion_token';
const USER_KEY = 'appgestion_user';

export interface UsuarioResponse {
  id: number;
  nombre: string;
  email: string;
  telefono?: string | null;
  /** ISO 8601 date-only (yyyy-MM-dd). */
  fechaNacimiento?: string | null;
  genero?: string | null;
  nacionalidadIso?: string | null;
  paisResidenciaIso?: string | null;
  emailNotifyBilling?: boolean;
  emailNotifyDocuments?: boolean;
  emailNotifyMarketing?: boolean;
  locale?: string;
  timeZone?: string;
  currencyCode?: string;
  rol: string;
  activo: boolean;
  fechaCreacion: string;
  subscriptionStatus?: string;
  trialEndDate?: string;
  /** ISO 8601; fin periodo facturación Stripe. */
  subscriptionCurrentPeriodEnd?: string | null;
  /** Cliente Stripe existente → portal de facturación. */
  billingPortalAvailable?: boolean;
  canWrite?: boolean;
  totpEnabled?: boolean;
  totpEnrollmentPending?: boolean;
  /** Claves de condiciones activas por defecto en presupuestos nuevos (GET /auth/me). */
  condicionesPresupuestoPredeterminadas?: string[];
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  /** Siempre alineado con `environment.apiUrl` (evita URL congelada al inyectar el servicio). */
  private get authRoot(): string {
    return `${environment.apiUrl}/auth`;
  }

  private tokenSignal = signal<string | null>(this.getStoredToken());
  private userSignal = signal<AuthResponse | null>(this.getStoredUser());
  /** Evita bucles de 401 durante POST /auth/logout y peticiones paralelas al cerrar sesión. */
  private logoutInProgress = false;

  token = this.tokenSignal.asReadonly();
  user = this.userSignal.asReadonly();
  isAuthenticated = computed(() => !!this.tokenSignal());
  canWrite = computed(() => this.userSignal()?.canWrite ?? false);
  /** Rol de aplicación (p. ej. ADMIN para rutas de administración). Los permisos de negocio los marca la suscripción. */
  businessRole = computed(() => (this.userSignal()?.rol ?? 'USER').toUpperCase());
  /** Puede crear/editar/borrar datos: solo si la suscripción / prueba lo permiten (canWrite). */
  canMutate = computed(() => this.canWrite());

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  login(credentials: LoginRequest): Observable<AuthResponse> {
    const body: Record<string, unknown> = {
      email: credentials.email,
      password: credentials.password,
      clientInfo: credentials.clientInfo ?? buildDeviceClientInfo(),
    };
    const code = credentials.totpCode?.trim();
    if (code) {
      body['totpCode'] = code;
    }
    return this.http.post<AuthResponse>(`${this.authRoot}/login`, body).pipe(
      tap((response) => this.handleAuthSuccess(response))
    );
  }

  register(data: RegisterRequest): Observable<AuthResponse> {
    const payload: Record<string, unknown> = {
      nombre: data.nombre,
      email: data.email,
      password: data.password,
      clientInfo: data.clientInfo ?? buildDeviceClientInfo(),
    };
    if (data.rol) payload['rol'] = data.rol;
    const ref = data.referralToken?.trim();
    if (ref) payload['referralToken'] = ref;
    return this.http.post<AuthResponse>(`${this.authRoot}/register`, payload).pipe(
      tap((response) => this.handleAuthSuccess(response))
    );
  }

  verifyInviteToken(token: string): Observable<InviteVerifyResponse> {
    return this.http.get<InviteVerifyResponse>(`${this.authRoot}/invite/verify`, {
      params: { token },
    });
  }

  acceptInvite(body: { token: string; nombre: string; password: string }): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.authRoot}/invite/accept`, {
        ...body,
        clientInfo: buildDeviceClientInfo(),
      })
      .pipe(tap((response) => this.handleAuthSuccess(response)));
  }

  sendInvitation(email: string): Observable<void> {
    return this.http.post<void>(`${this.authRoot}/invitations`, { email });
  }

  loginWithGoogle(idToken: string, totpCode?: string | null): Observable<AuthResponse> {
    const body: { idToken: string; totpCode?: string; clientInfo: ReturnType<typeof buildDeviceClientInfo> } = {
      idToken,
      clientInfo: buildDeviceClientInfo(),
    };
    const code = totpCode?.trim();
    if (code) {
      body.totpCode = code;
    }
    return this.http.post<AuthResponse>(`${this.authRoot}/google`, body).pipe(
      tap((response) => this.handleAuthSuccess(response))
    );
  }

  listSessions(): Observable<SesionDispositivoDto[]> {
    return this.http.get<SesionDispositivoDto[]>(`${this.authRoot}/sessions`);
  }

  revokeSession(sessionId: string): Observable<void> {
    return this.http.delete<void>(`${this.authRoot}/sessions/${encodeURIComponent(sessionId)}`);
  }

  revokeOtherSessions(): Observable<{ revokedCount: number }> {
    return this.http.delete<{ revokedCount: number }>(`${this.authRoot}/sessions/others`);
  }

  startTotpSetup(): Observable<TotpSetupStartResponse> {
    return this.http.post<TotpSetupStartResponse>(`${this.authRoot}/totp/setup/start`, {});
  }

  confirmTotpSetup(code: string): Observable<UsuarioResponse> {
    return this.http.post<UsuarioResponse>(`${this.authRoot}/totp/setup/confirm`, { code }).pipe(
      tap((me) => this.applyUsuarioToStoredUser(me))
    );
  }

  cancelTotpSetup(): Observable<void> {
    return this.http.post<void>(`${this.authRoot}/totp/setup/cancel`, {});
  }

  disableTotp(body: { currentPassword: string; totpCode: string }): Observable<void> {
    return this.http.post<void>(`${this.authRoot}/totp/disable`, body);
  }

  forgotPassword(email: string): Observable<void> {
    return this.http.post<void>(`${this.authRoot}/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${this.authRoot}/reset-password`, { token, newPassword });
  }

  /** Cambio de contraseña con sesión activa (POST /auth/change-password). */
  changePassword(body: { currentPassword: string; newPassword: string }): Observable<void> {
    return this.http.post<void>(`${this.authRoot}/change-password`, body);
  }

  refreshUser(): Observable<UsuarioResponse | null> {
    return this.http.get<UsuarioResponse>(`${this.authRoot}/me`).pipe(
      tap((me) => this.applyUsuarioToStoredUser(me)),
      catchError(() => of(null))
    );
  }

  /** Actualiza datos de perfil del usuario autenticado (PATCH /auth/profile). */
  updateProfile(body: {
    nombre: string;
    telefono: string;
    fechaNacimiento: string | null;
    genero: string | null;
    nacionalidadIso: string | null;
    paisResidenciaIso: string | null;
  }): Observable<UsuarioResponse> {
    const tel = body.telefono.trim();
    const payload = {
      nombre: body.nombre,
      telefono: tel.length === 0 ? null : tel,
      fechaNacimiento: body.fechaNacimiento,
      genero: body.genero,
      nacionalidadIso: body.nacionalidadIso,
      paisResidenciaIso: body.paisResidenciaIso,
    };
    return this.http.patch<UsuarioResponse>(`${this.authRoot}/profile`, payload).pipe(
      tap((me) => this.applyUsuarioToStoredUser(me))
    );
  }

  /** Preferencias de correo de la cuenta (PATCH /auth/account-settings). */
  updateAccountSettings(body: {
    emailNotifyBilling: boolean;
    emailNotifyDocuments: boolean;
    emailNotifyMarketing: boolean;
  }): Observable<UsuarioResponse> {
    return this.http.patch<UsuarioResponse>(`${this.authRoot}/account-settings`, body).pipe(
      tap((me) => this.applyUsuarioToStoredUser(me))
    );
  }

  /** Idioma, zona horaria y moneda (PATCH /auth/preferences). */
  updatePreferences(body: { locale: string; timeZone: string; currencyCode: string }): Observable<UsuarioResponse> {
    return this.http.patch<UsuarioResponse>(`${this.authRoot}/preferences`, body).pipe(
      tap((me) => this.applyUsuarioToStoredUser(me))
    );
  }

  private applyUsuarioToStoredUser(me: UsuarioResponse): void {
    const current = this.userSignal();
    if (!current) return;
    const preservedSessionId = current.sessionId ?? readJwtSessionId(this.getToken());
    const updated: AuthResponse = {
      ...current,
      rol: me.rol ?? current.rol,
      nombre: me.nombre ?? current.nombre,
      subscriptionStatus: me.subscriptionStatus,
      trialEndDate: me.trialEndDate,
      subscriptionCurrentPeriodEnd: me.subscriptionCurrentPeriodEnd,
      billingPortalAvailable: me.billingPortalAvailable,
      canWrite: me.canWrite ?? false,
    };
    // Teléfono: si el API no envía la propiedad (clientes antiguos), no tocar.
    if (me.telefono !== undefined) {
      updated.telefono =
        me.telefono != null && String(me.telefono).trim() !== '' ? String(me.telefono).trim() : undefined;
    }
    if (me.emailNotifyBilling !== undefined) {
      updated.emailNotifyBilling = me.emailNotifyBilling;
    }
    if (me.emailNotifyDocuments !== undefined) {
      updated.emailNotifyDocuments = me.emailNotifyDocuments;
    }
    if (me.emailNotifyMarketing !== undefined) {
      updated.emailNotifyMarketing = me.emailNotifyMarketing;
    }
    if (me.locale !== undefined) {
      updated.locale = me.locale;
    }
    if (me.timeZone !== undefined) {
      updated.timeZone = me.timeZone;
    }
    if (me.currencyCode !== undefined) {
      updated.currencyCode = me.currencyCode;
    }
    if (me.totpEnabled !== undefined) {
      updated.totpEnabled = me.totpEnabled;
    }
    if (preservedSessionId) {
      updated.sessionId = preservedSessionId;
    }
    localStorage.setItem(USER_KEY, JSON.stringify(updated));
    this.userSignal.set(updated);
  }

  /**
   * Revoca la sesión en servidor (si hay token) y limpia el almacenamiento local.
   */
  logout(): void {
    this.logoutInProgress = true;
    this.http
      .post<void>(`${this.authRoot}/logout`, {})
      .pipe(
        catchError(() => of(void 0)),
        finalize(() => {
          localStorage.removeItem(TOKEN_KEY);
          localStorage.removeItem(USER_KEY);
          this.tokenSignal.set(null);
          this.userSignal.set(null);
          this.logoutInProgress = false;
          this.router.navigate(['/login']);
        })
      )
      .subscribe();
  }

  /** Limpia sesión local sin llamar al API (p. ej. 401 en el interceptor). */
  clearSessionLocal(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.tokenSignal.set(null);
    this.userSignal.set(null);
  }

  isLogoutInProgress(): boolean {
    return this.logoutInProgress;
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  private handleAuthSuccess(response: AuthResponse): void {
    const sessionId = response.sessionId ?? readJwtSessionId(response.token);
    const normalized = {
      ...response,
      canWrite: response.canWrite ?? true,
      rol: response.rol ?? 'USER',
      sessionId,
    };
    localStorage.setItem(TOKEN_KEY, response.token);
    localStorage.setItem(USER_KEY, JSON.stringify(normalized));
    this.tokenSignal.set(response.token);
    this.userSignal.set(normalized);
  }

  private getStoredToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  private getStoredUser(): AuthResponse | null {
    const stored = localStorage.getItem(USER_KEY);
    if (!stored) {
      return null;
    }
    try {
      let u = JSON.parse(stored) as AuthResponse;
      const token = localStorage.getItem(TOKEN_KEY);
      const sid = readJwtSessionId(token);
      if (sid && !u.sessionId) {
        u = { ...u, sessionId: sid };
        localStorage.setItem(USER_KEY, JSON.stringify(u));
      }
      return u;
    } catch {
      return null;
    }
  }
}
