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
  let navigate: ReturnType<typeof vi.fn>;
  let snackBarOpen: ReturnType<typeof vi.fn>;
  let translateInstant: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    getToken = vi.fn();
    clearSessionLocal = vi.fn();
    isLogoutInProgress = vi.fn().mockReturnValue(false);
    navigate = vi.fn();
    snackBarOpen = vi.fn();
    translateInstant = vi.fn((key: string) => key);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        {
          provide: AuthService,
          useValue: { getToken, clearSessionLocal, isLogoutInProgress },
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
