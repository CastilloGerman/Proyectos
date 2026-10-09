import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { PresupuestoService } from '../../../core/services/presupuesto.service';
import { PresupuestoPublicoComponent } from './presupuesto-publico.component';

describe('PresupuestoPublicoComponent', () => {
  it('waits three visible seconds before recording a view, once per load', () => {
    vi.useFakeTimers();
    const service = {
      getPublico: vi.fn(() => of({
        empresaNombre: 'Reformas', empresaLogoBase64: null, empresaLogoMimeType: null, numero: 7, fecha: '2026-01-01T00:00:00',
        clienteNombre: 'Ana', partidas: [], subtotal: 10, iva: 2.1, total: 12.1, notas: null, condiciones: [],
      })),
      marcarPublicoVisto: vi.fn(() => of(void 0)),
    };
    TestBed.configureTestingModule({
      imports: [PresupuestoPublicoComponent, TranslateModule.forRoot()],
      providers: [
        { provide: PresupuestoService, useValue: service },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'opaque-token' } } } },
      ],
    });
    const fixture = TestBed.createComponent(PresupuestoPublicoComponent);
    const component = fixture.componentInstance;
    component.ngOnInit();
    expect(component.budget?.numero).toBe(7);
    expect(service.marcarPublicoVisto).not.toHaveBeenCalled();
    vi.advanceTimersByTime(2999);
    expect(service.marcarPublicoVisto).not.toHaveBeenCalled();
    vi.advanceTimersByTime(1);
    expect(service.marcarPublicoVisto).toHaveBeenCalledTimes(1);
    expect(service.marcarPublicoVisto).toHaveBeenCalledWith('opaque-token');
    document.dispatchEvent(new Event('pointerdown'));
    expect(service.marcarPublicoVisto).toHaveBeenCalledTimes(1);
    fixture.destroy();
    vi.useRealTimers();
  });

  it('registers a view immediately on first interaction and adds/removes privacy metadata', () => {
    const service = {
      getPublico: vi.fn(() => of({
        empresaNombre: 'Reformas', empresaLogoBase64: null, empresaLogoMimeType: null, numero: 7, fecha: '2026-01-01T00:00:00',
        clienteNombre: 'Ana', partidas: [], subtotal: 10, iva: 2.1, total: 12.1, notas: null, condiciones: [],
      })),
      marcarPublicoVisto: vi.fn(() => of(void 0)),
    };
    TestBed.configureTestingModule({
      imports: [PresupuestoPublicoComponent, TranslateModule.forRoot()],
      providers: [
        { provide: PresupuestoService, useValue: service },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'opaque-token' } } } },
      ],
    });
    const fixture = TestBed.createComponent(PresupuestoPublicoComponent);
    fixture.componentInstance.ngOnInit();
    expect(document.querySelector('meta[name="robots"]')?.getAttribute('content')).toBe('noindex,nofollow');
    expect(document.querySelector('meta[name="referrer"]')?.getAttribute('content')).toBe('no-referrer');
    document.dispatchEvent(new Event('keydown'));
    expect(service.marcarPublicoVisto).toHaveBeenCalledTimes(1);
    fixture.destroy();
    expect(document.querySelector('meta[name="robots"]')).toBeNull();
    expect(document.querySelector('meta[name="referrer"]')).toBeNull();
  });

  it('pauses the visibility timer while the tab is hidden', () => {
    vi.useFakeTimers();
    const originalVisibility = Object.getOwnPropertyDescriptor(document, 'visibilityState');
    Object.defineProperty(document, 'visibilityState', { configurable: true, value: 'hidden' });
    const service = {
      getPublico: vi.fn(() => of({
        empresaNombre: 'Reformas', empresaLogoBase64: null, empresaLogoMimeType: null, numero: 7, fecha: '2026-01-01T00:00:00',
        clienteNombre: 'Ana', partidas: [], subtotal: 10, iva: 2.1, total: 12.1, notas: null, condiciones: [],
      })),
      marcarPublicoVisto: vi.fn(() => of(void 0)),
    };
    TestBed.configureTestingModule({
      imports: [PresupuestoPublicoComponent, TranslateModule.forRoot()],
      providers: [
        { provide: PresupuestoService, useValue: service },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'opaque-token' } } } },
      ],
    });
    const fixture = TestBed.createComponent(PresupuestoPublicoComponent);
    fixture.componentInstance.ngOnInit();
    vi.advanceTimersByTime(5000);
    expect(service.marcarPublicoVisto).not.toHaveBeenCalled();
    Object.defineProperty(document, 'visibilityState', { configurable: true, value: 'visible' });
    document.dispatchEvent(new Event('visibilitychange'));
    vi.advanceTimersByTime(2999);
    expect(service.marcarPublicoVisto).not.toHaveBeenCalled();
    vi.advanceTimersByTime(1);
    expect(service.marcarPublicoVisto).toHaveBeenCalledTimes(1);
    fixture.destroy();
    if (originalVisibility) Object.defineProperty(document, 'visibilityState', originalVisibility);
    else Reflect.deleteProperty(document, 'visibilityState');
    vi.useRealTimers();
  });

  it('shows one unavailable state for an invalid public link', () => {
    TestBed.configureTestingModule({
      imports: [PresupuestoPublicoComponent, TranslateModule.forRoot()],
      providers: [
        { provide: PresupuestoService, useValue: { getPublico: () => throwError(() => ({ status: 404 })), marcarPublicoVisto: vi.fn() } },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'bad-token' } } } },
      ],
    });
    const component = TestBed.createComponent(PresupuestoPublicoComponent).componentInstance;
    component.ngOnInit();
    expect(component.unavailable).toBe(true);
    expect(component.networkError).toBe(false);
  });
});
