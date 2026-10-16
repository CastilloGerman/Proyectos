import { registerLocaleData } from '@angular/common';
import localeEs from '@angular/common/locales/es';
import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideRouter } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { PresupuestoService } from '../../../core/services/presupuesto.service';
import { PresupuestoSeguimientoComponent } from './presupuesto-seguimiento.component';

registerLocaleData(localeEs, 'es-ES');

describe('PresupuestoSeguimientoComponent', () => {
  const createService = () => ({
    crearEnlace: vi.fn(() => of({ url: 'https://example.test/p/new-token', expiraAt: '2026-12-01' })),
    regenerarEnlace: vi.fn(() => of({ url: 'https://example.test/p/replacement', expiraAt: '2026-12-01' })),
    revocarEnlace: vi.fn(() => of(void 0)),
    silenciarSeguimiento: vi.fn(() => of(void 0)),
    reactivarSeguimiento: vi.fn(() => of(void 0)),
    estadoEnlace: vi.fn(() => of({
      activo: true, expiraAt: '2026-12-01', primeraVistaAt: null, ultimaVistaAt: null, numVistas: 0, enlacesActivos: 2,
    })),
  });

  const mount = (service = createService(), confirmed = true) => {
    const snackBar = { open: vi.fn() };
    const dialog = { open: vi.fn(() => ({ afterClosed: () => of(confirmed) })) };
    TestBed.configureTestingModule({
      imports: [PresupuestoSeguimientoComponent, TranslateModule.forRoot()],
      providers: [
        provideRouter([]),
        { provide: PresupuestoService, useValue: service },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: MatDialog, useValue: dialog },
      ],
    });
    TestBed.overrideProvider(MatSnackBar, { useValue: snackBar });
    TestBed.overrideProvider(MatDialog, { useValue: dialog });
    const fixture = TestBed.createComponent(PresupuestoSeguimientoComponent);
    return { fixture, component: fixture.componentInstance, snackBar, dialog };
  };

  it('creates an additional link without regenerating or revoking shared links', () => {
    const service = createService();
    const { fixture, component } = mount(service);
    component.presupuestoId = 42;

    component.create();
    fixture.detectChanges();

    expect(service.crearEnlace).toHaveBeenCalledWith(42);
    expect(service.regenerarEnlace).not.toHaveBeenCalled();
    expect(service.revocarEnlace).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('.url')?.textContent).toContain('https://example.test/p/new-token');
  });

  it('regenerates only after explicit confirmation and leaves revocation to that endpoint', () => {
    const service = createService();
    const gate = { confirmed: false };
    const snackBar = { open: vi.fn() };
    const dialog = { open: vi.fn(() => ({ afterClosed: () => of(gate.confirmed) })) };
    TestBed.configureTestingModule({
      imports: [PresupuestoSeguimientoComponent, TranslateModule.forRoot()],
      providers: [
        provideRouter([]),
        { provide: PresupuestoService, useValue: service },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: MatDialog, useValue: dialog },
      ],
    });
    TestBed.overrideProvider(MatSnackBar, { useValue: snackBar });
    TestBed.overrideProvider(MatDialog, { useValue: dialog });
    const fixture = TestBed.createComponent(PresupuestoSeguimientoComponent);
    const component = fixture.componentInstance;
    component.presupuestoId = 42;

    component.regenerate();
    expect(service.regenerarEnlace).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('.url')).toBeNull();

    gate.confirmed = true;
    component.regenerate();
    fixture.detectChanges();

    expect(service.regenerarEnlace).toHaveBeenCalledWith(42);
    expect(service.crearEnlace).not.toHaveBeenCalled();
    expect(service.revocarEnlace).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('.url')?.textContent).toContain('https://example.test/p/replacement');
  });

  it('shows the translated configured active-link limit message on HTTP 409', () => {
    const service = createService();
    service.crearEnlace.mockImplementation(() => throwError(() => ({ status: 409 })));
    const { fixture, component, snackBar } = mount(service);
    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('es', {
      'publicBudget.linkLimitReached': 'Has alcanzado el límite de enlaces activos',
      'common.close': 'Cerrar',
    });
    translate.use('es');
    component.presupuestoId = 42;

    component.create();
    fixture.detectChanges();

    expect(snackBar.open).toHaveBeenCalledWith(
      'Has alcanzado el límite de enlaces activos',
      'Cerrar',
      { duration: 4000 },
    );
    expect(fixture.nativeElement.querySelector('.url')).toBeNull();
  });

  it('uses the send-panel phone normalization and hides an invalid phone link', () => {
    const { fixture, component } = mount();
    component.presupuestoId = 42;
    component.clientPhone = '612 345 678';
    component.clientCountry = 'ES';
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('a[href="tel:+34612345678"]')).not.toBeNull();

    component.clientPhone = '123';
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('a[href^="tel:"]')).toBeNull();
  });

  it('silences and reactivates follow-up through the matching endpoint', () => {
    const service = createService();
    const { fixture, component } = mount(service);
    component.presupuestoId = 42;
    component.toggleSilenced();
    fixture.detectChanges();
    expect(service.silenciarSeguimiento).toHaveBeenCalledWith(42);
    expect(fixture.nativeElement.textContent).toContain('budgetFollowSettings.reactivate');
    component.toggleSilenced();
    fixture.detectChanges();
    expect(service.reactivarSeguimiento).toHaveBeenCalledWith(42);
    expect(fixture.nativeElement.textContent).toContain('budgetFollowSettings.silence');
  });

  it('keeps silence and resend actions available before an alert exists', () => {
    const service = createService();
    const { fixture } = mount(service);
    fixture.componentInstance.presupuestoId = 42;
    fixture.detectChanges();

    const labels = Array.from(fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>)
      .map(button => button.textContent?.replace(/\s+/g, ' ').trim() ?? '');
    expect(labels.some(label => label.includes('budgetFollowSettings.silence'))).toBe(true);
    expect(labels.some(label => label.includes('budgetFollowSettings.resend'))).toBe(true);
  });

  it('emits the request to reopen the existing send panel', () => {
    const { fixture, component } = mount();
    component.presupuestoId = 42;
    const resend = vi.fn();
    component.resend.subscribe(resend);
    fixture.detectChanges();

    const resendButton = Array.from(fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>)
      .find(button => button.textContent?.includes('budgetFollowSettings.resend'));
    resendButton?.click();
    fixture.detectChanges();

    expect(resend).toHaveBeenCalledOnce();
  });
});
