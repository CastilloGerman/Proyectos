import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslateService } from '@ngx-translate/core';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let getToken: ReturnType<typeof vi.fn>;
  let clearSessionLocal: ReturnType<typeof vi.fn>;
  let isLogoutInProgress: ReturnType<typeof vi.fn>;
  let consumeExpiredSessionNotice: ReturnType<typeof vi.fn>;
  let navigate: ReturnType<typeof vi.fn>;
  let snackBarOpen: ReturnType<typeof vi.fn>;
  let translateInstant: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    getToken = vi.fn();
    clearSessionLocal = vi.fn();
    isLogoutInProgress = vi.fn().mockReturnValue(false);
    consumeExpiredSessionNotice = vi.fn().mockReturnValue(true);
    navigate = vi.fn();
    snackBarOpen = vi.fn();
    translateInstant = vi.fn((key: string) => key);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        {
          provide: AuthService,
          useValue: { getToken, clearSessionLocal, isLogoutInProgress, consumeExpiredSessionNotice },
        },
        { provide: Router, useValue: { navigate } },
        { provide: MatSnackBar, useValue: { open: snackBarOpen } },
        { provide: TranslateService, useValue: { instant: translateInstant } },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('adds Authorization header when token is present', () => {
    getToken.mockReturnValue('my-jwt-token');
    http.get('/api/clientes').subscribe();

    const req = httpMock.expectOne('/api/clientes');
    expect(req.request.headers.get('Authorization')).toBe('Bearer my-jwt-token');
    req.flush([]);
  });

  it('SEC-07: no envia el JWT a URLs que no son de la API', () => {
    getToken.mockReturnValue('my-jwt-token');
    http.get('https://terceros.example.com/recurso').subscribe();

    const req = httpMock.expectOne('https://terceros.example.com/recurso');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush([]);
  });

  it('does not add Authorization header when token is absent', () => {
    getToken.mockReturnValue(null);
    http.get('/api/clientes').subscribe();

    const req = httpMock.expectOne('/api/clientes');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush([]);
  });

  it('clears session and redirects on 401 for protected routes', () => {
    getToken.mockReturnValue('expired-token');
    let error: HttpErrorResponse | undefined;
    http.get('/api/facturas').subscribe({
      error: (err) => {
        error = err;
      },
    });

    const req = httpMock.expectOne('/api/facturas');
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(clearSessionLocal).toHaveBeenCalledTimes(1);
    expect(navigate).toHaveBeenCalledWith(['/login']);
    expect(snackBarOpen).toHaveBeenCalledWith('shell.snackbarSessionExpired', 'common.close', {
      duration: 4000,
    });
    expect(error?.status).toBe(401);
  });

  it('does not clear session on 401 for /auth/login', () => {
    getToken.mockReturnValue(null);
    http.post('/api/auth/login', {}).subscribe({ error: () => {} });

    const req = httpMock.expectOne('/api/auth/login');
    req.flush('Bad credentials', { status: 401, statusText: 'Unauthorized' });

    expect(clearSessionLocal).not.toHaveBeenCalled();
    expect(navigate).not.toHaveBeenCalled();
    expect(snackBarOpen).not.toHaveBeenCalled();
  });

  it('does not clear session on 401 for /auth/register', () => {
    getToken.mockReturnValue(null);
    http.post('/api/auth/register', {}).subscribe({ error: () => {} });

    const req = httpMock.expectOne('/api/auth/register');
    req.flush('Conflict', { status: 401, statusText: 'Unauthorized' });

    expect(clearSessionLocal).not.toHaveBeenCalled();
    expect(navigate).not.toHaveBeenCalled();
  });

  it('does not clear session on 401 for /auth/logout', () => {
    getToken.mockReturnValue('tok');
    http.post('/api/auth/logout', {}).subscribe({ error: () => {} });

    const req = httpMock.expectOne('/api/auth/logout');
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(clearSessionLocal).not.toHaveBeenCalled();
    expect(navigate).not.toHaveBeenCalled();
  });

  it('does not clear session on 401 when logout is in progress', () => {
    getToken.mockReturnValue('tok');
    isLogoutInProgress.mockReturnValue(true);
    http.get('/api/clientes').subscribe({ error: () => {} });

    const req = httpMock.expectOne('/api/clientes');
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(clearSessionLocal).not.toHaveBeenCalled();
    expect(navigate).not.toHaveBeenCalled();
  });

  it('does not call the API when the stored JWT is already expired', () => {
    const header = btoa(JSON.stringify({ alg: 'HS256' }));
    const body = btoa(JSON.stringify({ exp: 1 }));
    getToken.mockReturnValue(`${header}.${body}.sig`);
    let error: HttpErrorResponse | undefined;
    http.get('/api/facturas').subscribe({
      error: (err) => {
        error = err;
      },
    });

    httpMock.expectNone('/api/facturas');
    expect(consumeExpiredSessionNotice).toHaveBeenCalledTimes(1);
    expect(navigate).toHaveBeenCalledWith(['/login']);
    expect(snackBarOpen).toHaveBeenCalledWith('shell.snackbarSessionExpired', 'common.close', {
      duration: 4000,
    });
    expect(error?.status).toBe(401);
  });

  it('retries a GET once when the database is waking up and keeps the session', async () => {
    vi.useFakeTimers();
    getToken.mockReturnValue('tok');
    let body: unknown;
    http.get('/api/facturas').subscribe((value) => {
      body = value;
    });

    httpMock.expectOne('/api/facturas').flush('down', { status: 503, statusText: 'Service Unavailable' });
    await vi.advanceTimersByTimeAsync(800);
    httpMock.expectOne('/api/facturas').flush([{ id: 1 }]);

    expect(body).toEqual([{ id: 1 }]);
    expect(clearSessionLocal).not.toHaveBeenCalled();
    vi.useRealTimers();
  });

  it('does not clear session on 401 for public auth endpoints forgot-password and reset-password', () => {
    getToken.mockReturnValue(null);
    http.post('/api/auth/forgot-password', {}).subscribe({ error: () => {} });
    httpMock.expectOne('/api/auth/forgot-password').flush('Error', { status: 401, statusText: 'Unauthorized' });

    http.post('/api/auth/reset-password', {}).subscribe({ error: () => {} });
    httpMock.expectOne('/api/auth/reset-password').flush('Error', { status: 401, statusText: 'Unauthorized' });

    expect(clearSessionLocal).not.toHaveBeenCalled();
    expect(navigate).not.toHaveBeenCalled();
  });
});
