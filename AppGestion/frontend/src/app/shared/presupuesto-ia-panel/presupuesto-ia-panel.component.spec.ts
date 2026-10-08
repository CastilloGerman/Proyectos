import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { of, Subject, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { PresupuestoIaBorradorResponse, PresupuestoIaRequestError } from '../../core/models/presupuesto-ia.model';
import { PresupuestoIaService } from '../../core/services/presupuesto-ia.service';
import { PresupuestoIaPanelComponent } from './presupuesto-ia-panel.component';

describe('PresupuestoIaPanelComponent', () => {
  let fixture: ComponentFixture<PresupuestoIaPanelComponent>;
  let component: PresupuestoIaPanelComponent;
  const draft = {
    clienteId: null,
    clienteNombre: null,
    clienteTelefono: null,
    transcripcion: '',
    items: [],
    notaAdicional: null,
  } satisfies PresupuestoIaBorradorResponse;
  const service = { generarBorrador: vi.fn() };

  beforeEach(async () => {
    vi.clearAllMocks();
    service.generarBorrador.mockReturnValue(of(draft));
    await TestBed.configureTestingModule({
      imports: [PresupuestoIaPanelComponent, NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [{ provide: PresupuestoIaService, useValue: service }],
    }).compileComponents();
    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('es', { budQuick: { ai: {
      title: 'Describe la obra',
      descriptionLabel: 'Descripción',
      charCount: '{{count}} / 8.000 caracteres',
      generate: 'Generar con IA',
      generating: 'Preparando borrador…',
      progress: 'Organizando',
      draftReady: 'Borrador listo',
      disabled: 'IA desactivada',
      quotaHourly: 'Límite horario',
      quotaDaily: 'Límite diario',
      quotaAttempts: 'Límite de intentos',
      quotaProvider: 'Cuota del proveedor',
      providerError: 'Error temporal',
      tooLong: 'Texto demasiado largo',
      forbidden: 'Sin permiso',
      unauthorized: 'Sesión caducada',
      invalidRequest: 'Solicitud no válida',
      genericError: 'Error',
      confirmOverwrite: '¿Reemplazar partidas?',
      sentWarning: 'Presupuesto ya enviado',
      nonPendingWarning: 'Presupuesto ya no pendiente',
    } } });
    translate.setFallbackLang('es');
    translate.use('es');
    fixture = TestBed.createComponent(PresupuestoIaPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('emits an unpersisted draft and shows an existing-budget warning', () => {
    const output = vi.fn();
    component.draftGenerated.subscribe(output);
    component.text = 'Pintar cocina';
    component.clienteId = 7;
    component.warningMessage = 'Presupuesto ya enviado';
    fixture.detectChanges();

    component.generarBorrador();
    fixture.detectChanges();

    expect(service.generarBorrador).toHaveBeenCalledWith({ texto: 'Pintar cocina', clienteId: 7 });
    expect(output).toHaveBeenCalledWith(draft);
    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('Presupuesto ya enviado');
  });

  it('asks before replacing existing lines and keeps the draft when replacement is declined', () => {
    component.text = 'Texto existente';
    component.hasExistingItems = true;
    vi.spyOn(window, 'confirm').mockReturnValue(false);

    component.generarBorrador();

    expect(window.confirm).toHaveBeenCalledOnce();
    expect(service.generarBorrador).not.toHaveBeenCalled();
    expect(component.text).toBe('Texto existente');
  });

  it('enforces the character limit and preserves the full supported length', () => {
    component.text = 'x'.repeat(8001);
    fixture.detectChanges();
    expect((fixture.nativeElement.querySelector('.ai-generate') as HTMLButtonElement).disabled).toBe(true);
    component.generarBorrador();
    expect(service.generarBorrador).not.toHaveBeenCalled();

    component.text = 'x'.repeat(8000);
    fixture.detectChanges();
    expect((fixture.nativeElement.querySelector('.ai-generate') as HTMLButtonElement).disabled).toBe(false);
  });

  it('prevents overlapping requests and emits the pending response once', () => {
    const request = new Subject<PresupuestoIaBorradorResponse>();
    service.generarBorrador.mockReturnValue(request);
    const output = vi.fn();
    component.draftGenerated.subscribe(output);
    component.text = 'Trabajo';

    component.generarBorrador();
    component.generarBorrador();
    expect(service.generarBorrador).toHaveBeenCalledOnce();

    request.next(draft);
    request.complete();
    expect(output).toHaveBeenCalledOnce();
    expect(component.loading).toBe(false);
  });

  it.each([
    ['disabled', 'IA desactivada'],
    ['quota-hourly', 'Límite horario'],
    ['quota-daily', 'Límite diario'],
    ['quota-attempts', 'Límite de intentos'],
    ['quota-provider', 'Cuota del proveedor'],
    ['provider', 'Error temporal'],
  ] as const)('keeps the description and reports the localized %s error', (kind, expected) => {
    service.generarBorrador.mockReturnValue(throwError(() => new PresupuestoIaRequestError(kind, 503)));
    component.text = 'Texto que debe conservarse';

    component.generarBorrador();
    fixture.detectChanges();

    expect(component.text).toBe('Texto que debe conservarse');
    expect(component.errorMessage).toBe(expected);
    expect(fixture.nativeElement.querySelector('#ia-status')?.getAttribute('role')).toBe('alert');
    if (kind === 'disabled') {
      expect(component.unavailable).toBe(true);
      expect((fixture.nativeElement.querySelector('.ai-generate') as HTMLButtonElement).disabled).toBe(true);
    }
  });
});
