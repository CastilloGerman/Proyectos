import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService, UsuarioResponse } from './auth.service';
import { AuthResponse } from './models/auth.model';
import { environment } from '../../../environments/environment';

const TOKEN_KEY = 'appgestion_token';
const USER_KEY = 'appgestion_user';
const AUTH_ROOT = `${environment.apiUrl}/auth`;

function makeJwt(payload: Record<string, unknown>): string {
  const header = btoa(JSON.stringify({ alg: 'HS256' }))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
  const body = btoa(JSON.stringify(payload))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
  return `${header}.${body}.signature`;
}

function createMemoryStorage(): Storage {
  const store = new Map<string, string>();
  return {
    getItem: (key: string) => (store.has(key) ? store.get(key)! : null),
    setItem: (key: string, value: string) => {
      store.set(key, value);
    },
    removeItem: (key: string) => {
      store.delete(key);
    },
    clear: () => store.clear(),
    key: (index: number) => Array.from(store.keys())[index] ?? null,
    get length() {
      return store.size;
    },
  };
}

const mockAuthResponse: AuthResponse = {
  token: makeJwt({ sid: 'session-abc' }),
  type: 'Bearer',
  email: 'user@test.com',
  rol: 'USER',
  expiresAt: '2099-12-31T00:00:00Z',
  nombre: 'Test User',
  canWrite: true,
};

describe('AuthService', () => {
  let http: HttpTestingController;
  let service: AuthService;
  let navigate: ReturnType<typeof vi.fn>;
  let storage: Storage;

  beforeEach(() => {
    storage = createMemoryStorage();
    vi.stubGlobal('localStorage', storage);
    vi.stubGlobal('navigator', { userAgent: 'Vitest' });

    navigate = vi.fn();
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [{ provide: Router, useValue: { navigate } }],
    });
    http = TestBed.inject(HttpTestingController);
    service = TestBed.inject(AuthService);
  });

  afterEach(() => {
    http.verify();
    TestBed.resetTestingModule();
    vi.unstubAllGlobals();
  });

  it('should create', () => {
    expect(service).toBeTruthy();
  });

  describe('login', () => {
    it('posts credentials to /auth/login and persists session', () => {
      service.login({ email: 'user@test.com', password: 'secret' }).subscribe((res) => {
        expect(res.email).toBe('user@test.com');
      });

      const req = http.expectOne(`${AUTH_ROOT}/login`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body.email).toBe('user@test.com');
      expect(req.request.body.password).toBe('secret');
      expect(req.request.body.clientInfo).toBeDefined();
      expect(req.request.body.totpCode).toBeUndefined();
      req.flush(mockAuthResponse);

      expect(storage.getItem(TOKEN_KEY)).toBe(mockAuthResponse.token);
      expect(JSON.parse(storage.getItem(USER_KEY)!)).toMatchObject({
        email: 'user@test.com',
        rol: 'USER',
        canWrite: true,
        sessionId: 'session-abc',
      });
      expect(service.isAuthenticated()).toBe(true);
      expect(service.canWrite()).toBe(true);
      expect(service.businessRole()).toBe('USER');
    });

    it('includes totpCode when provided', () => {
      service.login({ email: 'user@test.com', password: 'secret', totpCode: ' 123456 ' }).subscribe();

      const req = http.expectOne(`${AUTH_ROOT}/login`);
      expect(req.request.body.totpCode).toBe('123456');
      req.flush(mockAuthResponse);
    });
  });

  describe('register', () => {
    it('posts registration payload to /auth/register', () => {
      service
        .register({ nombre: 'New User', email: 'new@test.com', password: 'pass123' })
        .subscribe();

      const req = http.expectOne(`${AUTH_ROOT}/register`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body.nombre).toBe('New User');
      expect(req.request.body.email).toBe('new@test.com');
      expect(req.request.body.password).toBe('pass123');
      expect(req.request.body.clientInfo).toBeDefined();
      req.flush(mockAuthResponse);

      expect(service.isAuthenticated()).toBe(true);
    });

    it('includes referralToken when provided', () => {
      service
        .register({
          nombre: 'Ref User',
          email: 'ref@test.com',
          password: 'pass123',
          referralToken: ' ref-token ',
        })
        .subscribe();

      const req = http.expectOne(`${AUTH_ROOT}/register`);
      expect(req.request.body.referralToken).toBe('ref-token');
      req.flush(mockAuthResponse);
    });
  });

  describe('logout', () => {
    it('posts to /auth/logout, clears storage and navigates to login', () => {
      storage.setItem(TOKEN_KEY, 'tok');
      storage.setItem(USER_KEY, JSON.stringify({ email: 'x@test.com' }));

      service.logout();
      expect(service.isLogoutInProgress()).toBe(true);

      const req = http.expectOne(`${AUTH_ROOT}/logout`);
      expect(req.request.method).toBe('POST');
      req.flush(null);

      expect(storage.getItem(TOKEN_KEY)).toBeNull();
      expect(storage.getItem(USER_KEY)).toBeNull();
      expect(service.isAuthenticated()).toBe(false);
      expect(service.isLogoutInProgress()).toBe(false);
      expect(navigate).toHaveBeenCalledWith(['/login']);
    });

    it('clears session even when logout request fails', () => {
      storage.setItem(TOKEN_KEY, 'tok');
      storage.setItem(USER_KEY, JSON.stringify({ email: 'x@test.com' }));

      service.logout();
      const req = http.expectOne(`${AUTH_ROOT}/logout`);
      req.flush('error', { status: 500, statusText: 'Server Error' });

      expect(storage.getItem(TOKEN_KEY)).toBeNull();
      expect(service.isAuthenticated()).toBe(false);
      expect(navigate).toHaveBeenCalledWith(['/login']);
    });
  });

  describe('clearSessionLocal', () => {
    it('removes token and user from storage without HTTP', () => {
      storage.setItem(TOKEN_KEY, 'tok');
      storage.setItem(USER_KEY, JSON.stringify({ email: 'x@test.com' }));
      service.clearSessionLocal();

      expect(storage.getItem(TOKEN_KEY)).toBeNull();
      expect(storage.getItem(USER_KEY)).toBeNull();
      expect(service.isAuthenticated()).toBe(false);
    });
  });

  describe('refreshUser', () => {
    beforeEach(() => {
      storage.setItem(TOKEN_KEY, mockAuthResponse.token);
      storage.setItem(USER_KEY, JSON.stringify(mockAuthResponse));
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        imports: [HttpClientTestingModule],
        providers: [{ provide: Router, useValue: { navigate: vi.fn() } }],
      });
      http = TestBed.inject(HttpTestingController);
      service = TestBed.inject(AuthService);
    });

    it('updates stored user from GET /auth/me', () => {
      const me: UsuarioResponse = {
        id: 1,
        nombre: 'Updated Name',
        email: 'user@test.com',
        rol: 'ADMIN',
        activo: true,
        fechaCreacion: '2024-01-01',
        canWrite: false,
        subscriptionStatus: 'ACTIVE',
      };

      service.refreshUser().subscribe((result) => {
        expect(result?.nombre).toBe('Updated Name');
      });

      const req = http.expectOne(`${AUTH_ROOT}/me`);
      expect(req.request.method).toBe('GET');
      req.flush(me);

      expect(service.user()?.nombre).toBe('Updated Name');
      expect(service.user()?.rol).toBe('ADMIN');
      expect(service.canWrite()).toBe(false);
    });

    it('returns null on HTTP error without throwing', () => {
      let result: UsuarioResponse | null | undefined;
      service.refreshUser().subscribe((r) => {
        result = r;
      });

      const req = http.expectOne(`${AUTH_ROOT}/me`);
      req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

      expect(result).toBeNull();
    });
  });

  describe('hydration from localStorage', () => {
    it('restores sessionId from JWT when user JSON lacks it', () => {
      const token = makeJwt({ sid: 'hydrated-sid' });
      storage.setItem(TOKEN_KEY, token);
      storage.setItem(
        USER_KEY,
        JSON.stringify({
          token,
          type: 'Bearer',
          email: 'stored@test.com',
          rol: 'USER',
          expiresAt: '2099-01-01',
        }),
      );

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        imports: [HttpClientTestingModule],
        providers: [{ provide: Router, useValue: { navigate: vi.fn() } }],
      });
      http = TestBed.inject(HttpTestingController);
      service = TestBed.inject(AuthService);

      expect(service.isAuthenticated()).toBe(true);
      expect(service.user()?.sessionId).toBe('hydrated-sid');
      const stored = JSON.parse(storage.getItem(USER_KEY)!);
      expect(stored.sessionId).toBe('hydrated-sid');
    });

    it('returns null user when stored JSON is invalid', () => {
      storage.setItem(TOKEN_KEY, 'tok');
      storage.setItem(USER_KEY, 'not-json');

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        imports: [HttpClientTestingModule],
        providers: [{ provide: Router, useValue: { navigate: vi.fn() } }],
      });
      http = TestBed.inject(HttpTestingController);
      service = TestBed.inject(AuthService);

      expect(service.user()).toBeNull();
    });
  });
});
