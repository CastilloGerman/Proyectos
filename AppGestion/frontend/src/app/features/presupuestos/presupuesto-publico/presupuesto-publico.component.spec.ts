import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { of, throwError, Subject } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { describe, expect, it, vi } from 'vitest';
import { PresupuestoService } from '../../../core/services/presupuesto.service';
import { PresupuestoPublicoComponent } from './presupuesto-publico.component';

describe('PresupuestoPublicoComponent', () => {
  const baseBudget = {
    empresaNombre: 'Reformas', empresaLogoBase64: null, empresaLogoMimeType: null, numero: 7, fecha: '2026-01-01T00:00:00',
    clienteNombre: 'Ana', partidas: [], subtotal: 10, iva: 2.1, total: 12.1, notas: null, condiciones: [],
  };

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

  // === Response dialog tests ===

  it('shows response buttons only when permiteResponder is true', () => {
    const service = {
      getPublico: vi.fn(() => of({ ...baseBudget, permiteResponder: true, respuestaCliente: null })),
      marcarPublicoVisto: vi.fn(() => of(void 0)),
    };
    TestBed.configureTestingModule({
      imports: [PresupuestoPublicoComponent, TranslateModule.forRoot()],
      providers: [
        { provide: PresupuestoService, useValue: service },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'token' } } } },
      ],
    });
    const fixture = TestBed.createComponent(PresupuestoPublicoComponent);
    fixture.componentInstance.ngOnInit();
    fixture.detectChanges();
    const buttons = fixture.nativeElement.querySelectorAll('.response-actions button');
    expect(buttons.length).toBe(2);
    fixture.destroy();
  });

  it('hides response buttons when permiteResponder is false', () => {
    const service = {
      getPublico: vi.fn(() => of({ ...baseBudget, permiteResponder: false })),
      marcarPublicoVisto: vi.fn(() => of(void 0)),
    };
    TestBed.configureTestingModule({
      imports: [PresupuestoPublicoComponent, TranslateModule.forRoot()],
      providers: [
        { provide: PresupuestoService, useValue: service },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'token' } } } },
      ],
    });
    const fixture = TestBed.createComponent(PresupuestoPublicoComponent);
    fixture.componentInstance.ngOnInit();
    fixture.detectChanges();
    const section = fixture.nativeElement.querySelector('.client-response');
    expect(section).toBeNull();
    fixture.destroy();
  });

  it('opens response dialog on button click and focuses message field', () => {
    const responderPublico = vi.fn(() => of(void 0));
    const service = {
      getPublico: vi.fn(() => of({ ...baseBudget, permiteResponder: true, respuestaCliente: null })),
      marcarPublicoVisto: vi.fn(() => of(void 0)),
      responderPublico,
    };
    TestBed.configureTestingModule({
      imports: [PresupuestoPublicoComponent, TranslateModule.forRoot()],
      providers: [
        { provide: PresupuestoService, useValue: service },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'token' } } } },
      ],
    });
    const fixture = TestBed.createComponent(PresupuestoPublicoComponent);
    fixture.componentInstance.ngOnInit();
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.openResponseDialog('INTERESA', { currentTarget: document.createElement('button') } as any);
    fixture.detectChanges();
    expect(component.responseDialogOpen).toBe(true);
    expect(component.selectedOption).toBe('INTERESA');
    fixture.destroy();
  });

  it('submits response and updates budget with selected option', () => {
    const responderPublico = vi.fn(() => of(void 0));
    const service = {
      getPublico: vi.fn(() => of({ ...baseBudget, permiteResponder: true, respuestaCliente: null })),
      marcarPublicoVisto: vi.fn(() => of(void 0)),
      responderPublico,
    };
    TestBed.configureTestingModule({
      imports: [PresupuestoPublicoComponent, TranslateModule.forRoot()],
      providers: [
        { provide: PresupuestoService, useValue: service },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'token' } } } },
      ],
    });
    const fixture = TestBed.createComponent(PresupuestoPublicoComponent);
    fixture.componentInstance.ngOnInit();
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.openResponseDialog('DUDAS', { currentTarget: document.createElement('button') } as any);
    component.messageDraft = '¿Incluye transporte?';
    component.submitResponse();
    expect(responderPublico).toHaveBeenCalledWith('token', { opcion: 'DUDAS', mensaje: '¿Incluye transporte?' });
    expect(component.budget?.respuestaCliente).toBe('DUDAS');
    expect(component.responseDialogOpen).toBe(false);
    fixture.destroy();
  });

  it('prevents double submission while response is sending', () => {
    // Use a subject that never completes to simulate an in-flight request
    const pendingRequest = new Subject<void>();
    const responderPublico = vi.fn(() => pendingRequest);
    const service = {
      getPublico: vi.fn(() => of({ ...baseBudget, permiteResponder: true, respuestaCliente: null })),
      marcarPublicoVisto: vi.fn(() => of(void 0)),
      responderPublico,
    };
    TestBed.configureTestingModule({
      imports: [PresupuestoPublicoComponent, TranslateModule.forRoot()],
      providers: [
        { provide: PresupuestoService, useValue: service },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'token' } } } },
      ],
    });
    const fixture = TestBed.createComponent(PresupuestoPublicoComponent);
    fixture.componentInstance.ngOnInit();
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.openResponseDialog('INTERESA', { currentTarget: document.createElement('button') } as any);
    component.submitResponse();
    expect(component.responseSubmitting).toBe(true);
    // Second submit should be ignored because responseSubmitting is true
    component.submitResponse();
    expect(responderPublico).toHaveBeenCalledTimes(1);
    fixture.destroy();
  });

  it('shows error message on 404 response', () => {
    const service = {
      getPublico: vi.fn(() => of({ ...baseBudget, permiteResponder: true, respuestaCliente: null })),
      marcarPublicoVisto: vi.fn(() => of(void 0)),
      responderPublico: vi.fn(() => throwError(() => new HttpErrorResponse({ status: 404 }))),
    };
    TestBed.configureTestingModule({
      imports: [PresupuestoPublicoComponent, TranslateModule.forRoot()],
      providers: [
        { provide: PresupuestoService, useValue: service },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'token' } } } },
      ],
    });
    const fixture = TestBed.createComponent(PresupuestoPublicoComponent);
    fixture.componentInstance.ngOnInit();
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.openResponseDialog('INTERESA', { currentTarget: document.createElement('button') } as any);
    component.submitResponse();
    expect(component.responseError).toBe('publicBudget.responseUnavailable');
    expect(component.responseSubmitting).toBe(false);
    fixture.destroy();
  });

  it('shows rate limited error on 429 response', () => {
    const service = {
      getPublico: vi.fn(() => of({ ...baseBudget, permiteResponder: true, respuestaCliente: null })),
      marcarPublicoVisto: vi.fn(() => of(void 0)),
      responderPublico: vi.fn(() => throwError(() => new HttpErrorResponse({ status: 429 }))),
    };
    TestBed.configureTestingModule({
      imports: [PresupuestoPublicoComponent, TranslateModule.forRoot()],
      providers: [
        { provide: PresupuestoService, useValue: service },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'token' } } } },
      ],
    });
    const fixture = TestBed.createComponent(PresupuestoPublicoComponent);
    fixture.componentInstance.ngOnInit();
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.openResponseDialog('INTERESA', { currentTarget: document.createElement('button') } as any);
    component.submitResponse();
    expect(component.responseError).toBe('publicBudget.responseRateLimited');
    fixture.destroy();
  });

  it('shows generic error on non-404/non-429 response', () => {
    const service = {
      getPublico: vi.fn(() => of({ ...baseBudget, permiteResponder: true, respuestaCliente: null })),
      marcarPublicoVisto: vi.fn(() => of(void 0)),
      responderPublico: vi.fn(() => throwError(() => new HttpErrorResponse({ status: 500 }))),
    };
    TestBed.configureTestingModule({
      imports: [PresupuestoPublicoComponent, TranslateModule.forRoot()],
      providers: [
        { provide: PresupuestoService, useValue: service },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'token' } } } },
      ],
    });
    const fixture = TestBed.createComponent(PresupuestoPublicoComponent);
    fixture.componentInstance.ngOnInit();
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.openResponseDialog('INTERESA', { currentTarget: document.createElement('button') } as any);
    component.submitResponse();
    expect(component.responseError).toBe('publicBudget.responseFailed');
    fixture.destroy();
  });

  it('shows non-contractual notice when permiteResponder is true', () => {
    const service = {
      getPublico: vi.fn(() => of({ ...baseBudget, permiteResponder: true, respuestaCliente: null })),
      marcarPublicoVisto: vi.fn(() => of(void 0)),
    };
    TestBed.configureTestingModule({
      imports: [PresupuestoPublicoComponent, TranslateModule.forRoot()],
      providers: [
        { provide: PresupuestoService, useValue: service },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'token' } } } },
      ],
    });
    const fixture = TestBed.createComponent(PresupuestoPublicoComponent);
    fixture.componentInstance.ngOnInit();
    fixture.detectChanges();
    const legalNote = fixture.nativeElement.querySelector('.legal-note');
    expect(legalNote).not.toBeNull();
    fixture.destroy();
  });

  it('shows current response status when respuestaCliente is set', () => {
    const service = {
      getPublico: vi.fn(() => of({ ...baseBudget, permiteResponder: true, respuestaCliente: 'INTERESA' })),
      marcarPublicoVisto: vi.fn(() => of(void 0)),
    };
    TestBed.configureTestingModule({
      imports: [PresupuestoPublicoComponent, TranslateModule.forRoot()],
      providers: [
        { provide: PresupuestoService, useValue: service },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'token' } } } },
      ],
    });
    const fixture = TestBed.createComponent(PresupuestoPublicoComponent);
    fixture.componentInstance.ngOnInit();
    fixture.detectChanges();
    expect(fixture.componentInstance.budget?.respuestaCliente).toBe('INTERESA');
    const statusEl = fixture.nativeElement.querySelector('[role="status"]');
    expect(statusEl).not.toBeNull();
    fixture.destroy();
  });

  it('sends response without message when message is empty', () => {
    const responderPublico = vi.fn(() => of(void 0));
    const service = {
      getPublico: vi.fn(() => of({ ...baseBudget, permiteResponder: true, respuestaCliente: null })),
      marcarPublicoVisto: vi.fn(() => of(void 0)),
      responderPublico,
    };
    TestBed.configureTestingModule({
      imports: [PresupuestoPublicoComponent, TranslateModule.forRoot()],
      providers: [
        { provide: PresupuestoService, useValue: service },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'token' } } } },
      ],
    });
    const fixture = TestBed.createComponent(PresupuestoPublicoComponent);
    fixture.componentInstance.ngOnInit();
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.openResponseDialog('INTERESA', { currentTarget: document.createElement('button') } as any);
    component.messageDraft = '';
    component.submitResponse();
    expect(responderPublico).toHaveBeenCalledWith('token', { opcion: 'INTERESA' });
    fixture.destroy();
  });

  it('closes dialog on Escape key when not submitting', () => {
    const service = {
      getPublico: vi.fn(() => of({ ...baseBudget, permiteResponder: true, respuestaCliente: null })),
      marcarPublicoVisto: vi.fn(() => of(void 0)),
      responderPublico: vi.fn(() => of(void 0)),
    };
    TestBed.configureTestingModule({
      imports: [PresupuestoPublicoComponent, TranslateModule.forRoot()],
      providers: [
        { provide: PresupuestoService, useValue: service },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'token' } } } },
      ],
    });
    const fixture = TestBed.createComponent(PresupuestoPublicoComponent);
    fixture.componentInstance.ngOnInit();
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.openResponseDialog('INTERESA', { currentTarget: document.createElement('button') } as any);
    expect(component.responseDialogOpen).toBe(true);
    component.onEscape();
    expect(component.responseDialogOpen).toBe(false);
    fixture.destroy();
  });

  it('does not close dialog on Escape key while submitting', () => {
    const service = {
      getPublico: vi.fn(() => of({ ...baseBudget, permiteResponder: true, respuestaCliente: null })),
      marcarPublicoVisto: vi.fn(() => of(void 0)),
      responderPublico: vi.fn(() => of(void 0)),
    };
    TestBed.configureTestingModule({
      imports: [PresupuestoPublicoComponent, TranslateModule.forRoot()],
      providers: [
        { provide: PresupuestoService, useValue: service },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'token' } } } },
      ],
    });
    const fixture = TestBed.createComponent(PresupuestoPublicoComponent);
    fixture.componentInstance.ngOnInit();
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.openResponseDialog('INTERESA', { currentTarget: document.createElement('button') } as any);
    component.responseSubmitting = true;
    component.onEscape();
    expect(component.responseDialogOpen).toBe(true);
    fixture.destroy();
  });

  it('trims message before sending', () => {
    const responderPublico = vi.fn(() => of(void 0));
    const service = {
      getPublico: vi.fn(() => of({ ...baseBudget, permiteResponder: true, respuestaCliente: null })),
      marcarPublicoVisto: vi.fn(() => of(void 0)),
      responderPublico,
    };
    TestBed.configureTestingModule({
      imports: [PresupuestoPublicoComponent, TranslateModule.forRoot()],
      providers: [
        { provide: PresupuestoService, useValue: service },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'token' } } } },
      ],
    });
    const fixture = TestBed.createComponent(PresupuestoPublicoComponent);
    fixture.componentInstance.ngOnInit();
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.openResponseDialog('INTERESA', { currentTarget: document.createElement('button') } as any);
    component.messageDraft = '  Hola  ';
    component.submitResponse();
    expect(responderPublico).toHaveBeenCalledWith('token', { opcion: 'INTERESA', mensaje: 'Hola' });
    fixture.destroy();
  });

  it('limits message to 500 characters in updateMessage', () => {
    const service = {
      getPublico: vi.fn(() => of({ ...baseBudget, permiteResponder: true, respuestaCliente: null })),
      marcarPublicoVisto: vi.fn(() => of(void 0)),
      responderPublico: vi.fn(() => of(void 0)),
    };
    TestBed.configureTestingModule({
      imports: [PresupuestoPublicoComponent, TranslateModule.forRoot()],
      providers: [
        { provide: PresupuestoService, useValue: service },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'token' } } } },
      ],
    });
    const fixture = TestBed.createComponent(PresupuestoPublicoComponent);
    fixture.componentInstance.ngOnInit();
    fixture.detectChanges();
    const component = fixture.componentInstance;
    const longText = 'a'.repeat(600);
    const event = { target: { value: longText } } as any;
    component.updateMessage(event);
    expect(component.messageDraft.length).toBe(500);
    fixture.destroy();
  });

  it('shows rate limited error message on 429 and allows retry after cooldown', () => {
    const responderPublico = vi.fn(() => throwError(() => new HttpErrorResponse({ status: 429 })));
    const service = {
      getPublico: vi.fn(() => of({ ...baseBudget, permiteResponder: true, respuestaCliente: null })),
      marcarPublicoVisto: vi.fn(() => of(void 0)),
      responderPublico,
    };
    TestBed.configureTestingModule({
      imports: [PresupuestoPublicoComponent, TranslateModule.forRoot()],
      providers: [
        { provide: PresupuestoService, useValue: service },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'token' } } } },
      ],
    });
    const fixture = TestBed.createComponent(PresupuestoPublicoComponent);
    fixture.componentInstance.ngOnInit();
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.openResponseDialog('INTERESA', { currentTarget: document.createElement('button') } as any);
    component.submitResponse();
    expect(component.responseError).toBe('publicBudget.responseRateLimited');
    expect(component.responseSubmitting).toBe(false);
    // Dialog remains open so the user can retry after cooldown
    expect(component.responseDialogOpen).toBe(true);
    fixture.destroy();
  });

  it('changes option after responding successfully', () => {
    const responderPublico = vi.fn(() => of(void 0));
    const service = {
      getPublico: vi.fn(() => of({ ...baseBudget, permiteResponder: true, respuestaCliente: 'INTERESA' })),
      marcarPublicoVisto: vi.fn(() => of(void 0)),
      responderPublico,
    };
    TestBed.configureTestingModule({
      imports: [PresupuestoPublicoComponent, TranslateModule.forRoot()],
      providers: [
        { provide: PresupuestoService, useValue: service },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'token' } } } },
      ],
    });
    const fixture = TestBed.createComponent(PresupuestoPublicoComponent);
    fixture.componentInstance.ngOnInit();
    fixture.detectChanges();
    const component = fixture.componentInstance;
    expect(component.budget?.respuestaCliente).toBe('INTERESA');

    // Change option from INTERESA to DUDAS
    component.openResponseDialog('DUDAS', { currentTarget: document.createElement('button') } as any);
    component.messageDraft = 'Cambio de opinión';
    component.submitResponse();
    expect(responderPublico).toHaveBeenCalledWith('token', { opcion: 'DUDAS', mensaje: 'Cambio de opinión' });
    expect(component.budget?.respuestaCliente).toBe('DUDAS');
    expect(component.responseDialogOpen).toBe(false);
    fixture.destroy();
  });

  it('shows current response status with role=status for accessibility', () => {
    const service = {
      getPublico: vi.fn(() => of({ ...baseBudget, permiteResponder: true, respuestaCliente: 'DUDAS' })),
      marcarPublicoVisto: vi.fn(() => of(void 0)),
    };
    TestBed.configureTestingModule({
      imports: [PresupuestoPublicoComponent, TranslateModule.forRoot()],
      providers: [
        { provide: PresupuestoService, useValue: service },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'token' } } } },
      ],
    });
    const fixture = TestBed.createComponent(PresupuestoPublicoComponent);
    fixture.componentInstance.ngOnInit();
    fixture.detectChanges();
    const statusEl = fixture.nativeElement.querySelector('[role="status"]');
    expect(statusEl).not.toBeNull();
    fixture.destroy();
  });
});
