import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { ConfigService } from '../../../core/services/config.service';
import { SeguimientoPresupuestosComponent } from './seguimiento-presupuestos.component';

describe('SeguimientoPresupuestosComponent', () => {
  const defaults = { seguimientoActivo: true, diasEspera: 10, maxAvisos: 4, emailResumen: true };
  const setup = (service: { getSeguimientoPresupuestos: ReturnType<typeof vi.fn>; patchSeguimientoPresupuestos: ReturnType<typeof vi.fn> }) => {
    TestBed.configureTestingModule({
      imports: [SeguimientoPresupuestosComponent, TranslateModule.forRoot()],
      providers: [provideRouter([]), { provide: ConfigService, useValue: service }],
    });
    return TestBed.createComponent(SeguimientoPresupuestosComponent).componentInstance;
  };

  it('shows the configured default values before loading preferences', () => {
    const service = { getSeguimientoPresupuestos: vi.fn(() => of(defaults)), patchSeguimientoPresupuestos: vi.fn() };
    const component = setup(service);

    expect(component.form.getRawValue()).toEqual({
      seguimientoActivo: true,
      diasEspera: 3,
      maxAvisos: 2,
      emailResumen: false,
    });
  });

  it('loads user preferences into the form', () => {
    const service = { getSeguimientoPresupuestos: vi.fn(() => of(defaults)), patchSeguimientoPresupuestos: vi.fn() };
    const component = setup(service);
    component.ngOnInit();

    expect(service.getSeguimientoPresupuestos).toHaveBeenCalledOnce();
    expect(component.form.getRawValue()).toEqual(defaults);
    expect(component.loading).toBe(false);
    expect(component.form.pristine).toBe(true);
  });

  it('saves valid preferences and shows explicit success confirmation', () => {
    const service = { getSeguimientoPresupuestos: vi.fn(() => of(defaults)), patchSeguimientoPresupuestos: vi.fn(() => of(defaults)) };
    const component = setup(service);
    component.ngOnInit();
    component.form.markAsDirty();

    component.save();

    expect(service.patchSeguimientoPresupuestos).toHaveBeenCalledWith(defaults);
    expect(component.feedbackType).toBe('success');
    expect(component.feedback).toBe('budgetFollowSettings.saved');
    expect(component.form.pristine).toBe(true);
  });

  it('blocks values outside the configured ranges', () => {
    const service = { getSeguimientoPresupuestos: vi.fn(() => of(defaults)), patchSeguimientoPresupuestos: vi.fn() };
    const component = setup(service);
    component.form.controls.diasEspera.setValue(31);
    component.form.controls.maxAvisos.setValue(0);

    component.save();

    expect(component.form.invalid).toBe(true);
    expect(service.patchSeguimientoPresupuestos).not.toHaveBeenCalled();
  });

  it('reports load and save errors and allows retry', () => {
    const service = {
      getSeguimientoPresupuestos: vi.fn(() => throwError(() => new Error('offline'))),
      patchSeguimientoPresupuestos: vi.fn(() => throwError(() => new Error('offline'))),
    };
    const component = setup(service);
    component.load();
    expect(component.loadError).toBe(true);

    component.form.patchValue(defaults);
    component.save();
    expect(component.feedbackType).toBe('error');
    expect(component.feedback).toBe('budgetFollowSettings.saveError');
    expect(component.saving).toBe(false);
  });
});
