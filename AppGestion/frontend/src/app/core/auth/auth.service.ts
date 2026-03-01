import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, of } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest } from './models/auth.model';
import { environment } from '../../../environments/environment';

const TOKEN_KEY = 'appgestion_token';
const USER_KEY = 'appgestion_user';

export interface UsuarioResponse {
  id: number;
  nombre: string;
  email: string;
  rol: string;
  activo: boolean;
  fechaCreacion: string;
  subscriptionStatus?: string;
  trialEndDate?: string;
  canWrite?: boolean;
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

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap((response) => this.handleAuthSuccess(response))
    );
  }

  register(data: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, data).pipe(
      tap((response) => this.handleAuthSuccess(response))
    );
  }

  refreshUser(): Observable<UsuarioResponse | null> {
    return this.http.get<UsuarioResponse>(`${this.apiUrl}/me`).pipe(
      tap((me) => {
        const current = this.userSignal();
        if (current) {
          const updated: AuthResponse = {
            ...current,
            subscriptionStatus: me.subscriptionStatus,
            trialEndDate: me.trialEndDate,
            canWrite: me.canWrite ?? false,
          };
          localStorage.setItem(USER_KEY, JSON.stringify(updated));
          this.userSignal.set(updated);
        }
      }),
      catchError(() => of(null))
    );
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
    const normalized = { ...response, canWrite: response.canWrite ?? true };
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
