import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';
import { CookieConsentService } from './cookie-consent.service';

describe('CookieConsentService (SEC-32)', () => {
  beforeEach(() => {
    localStorage.removeItem('cookie_consent');
    TestBed.resetTestingModule();
  });

  it('sin decisión previa el estado es null (la analítica no se carga)', () => {
    expect(TestBed.inject(CookieConsentService).status()).toBeNull();
  });

  it('accept() persiste y expone "accepted"', () => {
    const svc = TestBed.inject(CookieConsentService);
    svc.accept();
    expect(svc.status()).toBe('accepted');
    expect(localStorage.getItem('cookie_consent')).toBe('accepted');
  });

  it('reject() persiste y expone "rejected"', () => {
    const svc = TestBed.inject(CookieConsentService);
    svc.reject();
    expect(svc.status()).toBe('rejected');
    expect(localStorage.getItem('cookie_consent')).toBe('rejected');
  });

  it('ignora valores desconocidos en localStorage', () => {
    localStorage.setItem('cookie_consent', 'whatever');
    expect(TestBed.inject(CookieConsentService).status()).toBeNull();
  });

  it('recupera una aceptación previa', () => {
    localStorage.setItem('cookie_consent', 'accepted');
    expect(TestBed.inject(CookieConsentService).status()).toBe('accepted');
  });
});
