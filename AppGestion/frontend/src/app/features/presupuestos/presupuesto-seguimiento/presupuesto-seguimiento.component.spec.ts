import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { PresupuestoService } from '../../../core/services/presupuesto.service';
import { PresupuestoSeguimientoComponent } from './presupuesto-seguimiento.component';

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

  it('creates an additional link without regenerating or revoking shared links', () => {
    const service = createService();
    TestBed.configureTestingModule({
      imports: [PresupuestoSeguimientoComponent, TranslateModule.forRoot()],
      providers: [provideRouter([]), { provide: PresupuestoService, useValue: service }],
    });
    const component = TestBed.createComponent(PresupuestoSeguimientoComponent).componentInstance;
    component.presupuestoId = 42;

    component.create();

    expect(service.crearEnlace).toHaveBeenCalledWith(42);
    expect(service.regenerarEnlace).not.toHaveBeenCalled();
    expect(service.revocarEnlace).not.toHaveBeenCalled();
    expect(component.url).toBe('https://example.test/p/new-token');
  });

  it('regenerates only after explicit action and revokes previous links on the server', () => {
    const service = createService();
    TestBed.configureTestingModule({
      imports: [PresupuestoSeguimientoComponent, TranslateModule.forRoot()],
      providers: [{ provide: PresupuestoService, useValue: service }],
    });
    const component = TestBed.createComponent(PresupuestoSeguimientoComponent).componentInstance;
    component.presupuestoId = 42;

    component.regenerate();

    expect(service.regenerarEnlace).toHaveBeenCalledWith(42);
    expect(service.crearEnlace).not.toHaveBeenCalled();
    expect(component.url).toBe('https://example.test/p/replacement');
  });

  it('shows the translated configured active-link limit message on HTTP 409', () => {
    const service = createService();
    service.crearEnlace.mockImplementation(() => throwError(() => ({ status: 409 })));
    TestBed.configureTestingModule({
      imports: [PresupuestoSeguimientoComponent, TranslateModule.forRoot()],
      providers: [{ provide: PresupuestoService, useValue: service }],
    });
    const component = TestBed.createComponent(PresupuestoSeguimientoComponent).componentInstance;
    component.presupuestoId = 42;

    component.create();

    expect(component.feedback).toBe('publicBudget.linkLimitReached');
  });

  it('uses the send-panel phone normalization and hides an invalid phone link', () => {
    TestBed.configureTestingModule({
      imports: [PresupuestoSeguimientoComponent, TranslateModule.forRoot()],
      providers: [{ provide: PresupuestoService, useValue: createService() }],
    });
    const component = TestBed.createComponent(PresupuestoSeguimientoComponent).componentInstance;
    component.clientPhone = '612 345 678';
    component.clientCountry = 'ES';
    expect(component.phoneHref).toBe('tel:+34612345678');
    component.clientPhone = '123';
    expect(component.phoneHref).toBe('');
  });

  it('silences and reactivates follow-up through the matching endpoint', () => {
    const service = createService();
    TestBed.configureTestingModule({
      imports: [PresupuestoSeguimientoComponent, TranslateModule.forRoot()],
      providers: [{ provide: PresupuestoService, useValue: service }],
    });
    const component = TestBed.createComponent(PresupuestoSeguimientoComponent).componentInstance;
    component.presupuestoId = 42;
    component.toggleSilenced();
    expect(service.silenciarSeguimiento).toHaveBeenCalledWith(42);
    expect(component.silenced).toBe(true);
    component.toggleSilenced();
    expect(service.reactivarSeguimiento).toHaveBeenCalledWith(42);
    expect(component.silenced).toBe(false);
  });

  it('keeps silence and resend actions available before an alert exists', () => {
    const service = createService();
    TestBed.configureTestingModule({
      imports: [PresupuestoSeguimientoComponent, TranslateModule.forRoot()],
      providers: [provideRouter([]), { provide: PresupuestoService, useValue: service }],
    });
    const fixture = TestBed.createComponent(PresupuestoSeguimientoComponent);
    fixture.componentInstance.presupuestoId = 42;
    fixture.detectChanges();

    const buttons = Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[];
    expect(buttons.map(button => button.textContent?.trim())).toContain('budgetFollowSettings.silence');
    expect(buttons.map(button => button.textContent?.trim())).toContain('budgetFollowSettings.resend');
  });

  it('emits the request to reopen the existing send panel', () => {
    TestBed.configureTestingModule({
      imports: [PresupuestoSeguimientoComponent, TranslateModule.forRoot()],
      providers: [{ provide: PresupuestoService, useValue: createService() }],
    });
    const component = TestBed.createComponent(PresupuestoSeguimientoComponent).componentInstance;
    const resend = vi.fn();
    component.resend.subscribe(resend);
    component.resend.emit();
    expect(resend).toHaveBeenCalledOnce();
  });
});
