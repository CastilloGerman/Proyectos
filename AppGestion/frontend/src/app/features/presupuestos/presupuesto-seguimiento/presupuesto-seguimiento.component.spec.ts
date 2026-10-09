import { TestBed } from '@angular/core/testing';
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
    estadoEnlace: vi.fn(() => of({
      activo: true, expiraAt: '2026-12-01', primeraVistaAt: null, ultimaVistaAt: null, numVistas: 0, enlacesActivos: 2,
    })),
  });

  it('creates an additional link without regenerating or revoking shared links', () => {
    const service = createService();
    TestBed.configureTestingModule({
      imports: [PresupuestoSeguimientoComponent, TranslateModule.forRoot()],
      providers: [{ provide: PresupuestoService, useValue: service }],
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
});
