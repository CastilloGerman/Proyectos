import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, of } from 'rxjs';
import {
  AuthResponse,
  InviteVerifyResponse,
  LoginRequest,
  RegisterRequest,
  TotpSetupStartResponse,
} from './models/auth.model';
import { environment } from '../../../environments/environment';

const TOKEN_KEY = 'appgestion_token';
const USER_KEY = 'appgestion_user';

export interface UsuarioResponse {
  id: number;
  nombre: string;
  email: string;
  telefono?: string | null;
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
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = `${environment.apiUrl}/auth`;

  private tokenSignal = signal<string | null>(this.getStoredToken());
  private userSignal = signal<AuthResponse | null>(this.getStoredUser());

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
    const body: Record<string, string> = {
      email: credentials.email,
      password: credentials.password,
    };
    const code = credentials.totpCode?.trim();
    if (code) {
      body['totpCode'] = code;
    }
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, body).pipe(
      tap((response) => this.handleAuthSuccess(response))
    );
  }

  register(data: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, data).pipe(
      tap((response) => this.handleAuthSuccess(response))
    );
  }

  verifyInviteToken(token: string): Observable<InviteVerifyResponse> {
    return this.http.get<InviteVerifyResponse>(`${this.apiUrl}/invite/verify`, {
      params: { token },
    });
  }

  acceptInvite(body: { token: string; nombre: string; password: string }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/invite/accept`, body).pipe(
      tap((response) => this.handleAuthSuccess(response))
    );
  }

  sendInvitation(email: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/invitations`, { email });
  }

  loginWithGoogle(idToken: string, totpCode?: string | null): Observable<AuthResponse> {
    const body: { idToken: string; totpCode?: string } = { idToken };
    const code = totpCode?.trim();
    if (code) {
      body.totpCode = code;
    }
    return this.http.post<AuthResponse>(`${this.apiUrl}/google`, body).pipe(
      tap((response) => this.handleAuthSuccess(response))
    );
  }

  startTotpSetup(): Observable<TotpSetupStartResponse> {
    return this.http.post<TotpSetupStartResponse>(`${this.apiUrl}/totp/setup/start`, {});
  }

  confirmTotpSetup(code: string): Observable<UsuarioResponse> {
    return this.http.post<UsuarioResponse>(`${this.apiUrl}/totp/setup/confirm`, { code }).pipe(
      tap((me) => this.applyUsuarioToStoredUser(me))
    );
  }

  cancelTotpSetup(): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/totp/setup/cancel`, {});
  }

  disableTotp(body: { currentPassword: string; totpCode: string }): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/totp/disable`, body);
  }

  forgotPassword(email: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/reset-password`, { token, newPassword });
  }

  /** Cambio de contraseña con sesión activa (POST /auth/change-password). */
  changePassword(body: { currentPassword: string; newPassword: string }): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/change-password`, body);
  }

  refreshUser(): Observable<UsuarioResponse | null> {
    return this.http.get<UsuarioResponse>(`${this.apiUrl}/me`).pipe(
      tap((me) => this.applyUsuarioToStoredUser(me)),
      catchError(() => of(null))
    );
  }

  /** Actualiza nombre y teléfono del usuario autenticado (PATCH /auth/profile). */
  updateProfile(body: { nombre: string; telefono: string }): Observable<UsuarioResponse> {
    const tel = body.telefono.trim();
    const payload = { nombre: body.nombre, telefono: tel.length === 0 ? null : tel };
    return this.http.patch<UsuarioResponse>(`${this.apiUrl}/profile`, payload).pipe(
      tap((me) => this.applyUsuarioToStoredUser(me))
    );
  }

  /** Preferencias de correo de la cuenta (PATCH /auth/account-settings). */
  updateAccountSettings(body: {
    emailNotifyBilling: boolean;
    emailNotifyDocuments: boolean;
    emailNotifyMarketing: boolean;
  }): Observable<UsuarioResponse> {
    return this.http.patch<UsuarioResponse>(`${this.apiUrl}/account-settings`, body).pipe(
      tap((me) => this.applyUsuarioToStoredUser(me))
    );
  }

  /** Idioma, zona horaria y moneda (PATCH /auth/preferences). */
  updatePreferences(body: { locale: string; timeZone: string; currencyCode: string }): Observable<UsuarioResponse> {
    return this.http.patch<UsuarioResponse>(`${this.apiUrl}/preferences`, body).pipe(
      tap((me) => this.applyUsuarioToStoredUser(me))
    );
  }

  private applyUsuarioToStoredUser(me: UsuarioResponse): void {
    const current = this.userSignal();
    if (!current) return;
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
    localStorage.setItem(USER_KEY, JSON.stringify(updated));
    this.userSignal.set(updated);
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.tokenSignal.set(null);
    this.userSignal.set(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  private handleAuthSuccess(response: AuthResponse): void {
    const normalized = {
      ...response,
      canWrite: response.canWrite ?? true,
      rol: response.rol ?? 'USER',
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
    return stored ? JSON.parse(stored) : null;
  }
}
