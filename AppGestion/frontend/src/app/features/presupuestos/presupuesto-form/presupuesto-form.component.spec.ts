import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { FormBuilder } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../../core/auth/auth.service';
import { PresupuestoIaBorradorResponse } from '../../../core/models/presupuesto-ia.model';
import { ClienteService } from '../../../core/services/cliente.service';
import { MaterialService } from '../../../core/services/material.service';
import { PresupuestoIaService } from '../../../core/services/presupuesto-ia.service';
import { PresupuestoService } from '../../../core/services/presupuesto.service';
import { Presupuesto } from '../../../core/models/presupuesto.model';
import { PresupuestoFormComponent } from './presupuesto-form.component';

describe('PresupuestoFormComponent AI review', () => {
  let fixture: ComponentFixture<PresupuestoFormComponent>;
  let component: PresupuestoFormComponent;
  const iaService = { generarBorrador: vi.fn() };
  const snackBar = { open: vi.fn() };

  beforeEach(async () => {
    iaService.generarBorrador.mockReturnValue(of(draft()));
    await TestBed.configureTestingModule({
      imports: [PresupuestoFormComponent, HttpClientTestingModule, NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        FormBuilder,
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: {
          paramMap: { get: () => null }, queryParamMap: { get: () => null },
        } } },
        { provide: Router, useValue: { navigate: vi.fn() } },
        { provide: AuthService, useValue: { canMutate: () => true } },
        { provide: PresupuestoService, useValue: {
          getCondicionesDisponibles: () => of([]),
          getMisCondicionesPredeterminadas: () => of([]),
          getResumenAnticipo: () => of(null),
        } },
        { provide: ClienteService, useValue: { getAll: () => of([]) } },
        { provide: MaterialService, useValue: { getAll: () => of([]), getTopUsados: () => of([]) } },
        { provide: PresupuestoIaService, useValue: iaService },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: MatDialog, useValue: { open: vi.fn() } },
      ],
    }).compileComponents();
    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('es', { budQuick: { ai: {
      title: 'Describe la obra', descriptionLabel: 'Descripción', priceTip: 'Indica el precio',
      charCount: '{{count}} caracteres', generate: 'Generar', generating: 'Generando',
      progress: 'Preparando', draftReady: 'Borrador listo', confirmSuggestion: 'Confirmar sugerencia',
      formPending: 'Confirma {{count}} líneas', missingPrice: 'Falta precio',
      missingQuantity: 'Falta cantidad', dictatedPrice: 'Precio dictado',
      approximatePrice: 'Precio aproximado', catalogPrice: 'En tu catálogo',
      pendingSummary: 'Pendientes {{price}} precio, {{quantity}} cantidad, {{review}} revisión',
      sentWarning: 'Este presupuesto ya se envió al cliente.',
      nonPendingWarning: 'Este presupuesto ya no está pendiente.',
      confirmSafe: 'Confirmar {{count}} seguras',
      safeDialogTitle: 'Revisa las sugerencias seguras',
      safeDialogDescription: 'Comprueba descripción, material y precio antes de confirmar.',
      confirmAllSafe: 'Confirmar todas',
      cancelSafe: 'Cancelar',
      submitBlock: {
        permission: 'Sin permiso', saving: 'Guardando', generating: 'Generando IA',
        validationPending: 'Validando', customer: 'Falta seleccionar o crear el cliente.',
        line: 'Añade una partida', review: 'Confirma {{count}} sugerencias de IA.',
        price: 'Falta precio en {{count}} sugerencias', quantity: 'Falta cantidad en {{count}} sugerencias',
        invalidQuantity: 'Cantidad no válida en la partida {{line}}.',
        invalidPrice: 'Precio no válido en la partida {{line}}.',
        invalidForm: 'Revisa el formulario',
      },
    }, aiPendingBlock: 'Revisa antes de guardar', aiHelp: {
      title: 'Cómo funciona Crear con IA', step1: 'Describe la obra', step2: 'La IA crea un borrador',
      step3: 'Revisa los precios', note: 'No incluyas datos personales',
    } }, budgetForm: { manualTasksTitle: 'Trabajos manuales' } });
    translate.use('es');
    fixture = TestBed.createComponent(PresupuestoFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads dictated prices into the regular form and blocks saving until each line is confirmed', () => {
    component.form.get('clienteId')?.setValue(7);
    component.textoObraIa = 'Pintura por 45 euros cada bote';
    component.generarConIa();
    fixture.detectChanges();

    expect(component.manualItems.at(0).get('precioUnitario')?.value).toBe(45);
    expect(component.manualItems.at(0).get('precioOrigen')?.value).toBe('dictado');
    expect(fixture.nativeElement.textContent).toContain('Precio dictado');
    expect(fixture.nativeElement.querySelector('.price-badge mat-icon')?.textContent.trim())
      .toBe('record_voice_over');
    expect(component.pendientesIa()).toEqual({ precio: 0, cantidad: 0, revision: 1 });
    expect(component.botonCrearDeshabilitado()).toBe(true);

    component.confirmarSugerenciaIa(0);
    fixture.detectChanges();
    expect(component.pendientesIa()).toEqual({ precio: 0, cantidad: 0, revision: 0 });
    expect(component.botonCrearDeshabilitado()).toBe(false);
  });

  it('enables create when the customer and all AI lines are complete and confirmed', () => {
    component.form.get('clienteId')?.setValue(7);
    component.cargarBorradorIa(draft());
    component.confirmarSugerenciaIa(0);
    fixture.detectChanges();

    expect(component.motivosBloqueoCrear()).toEqual([]);
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBe(false);
    expect(fixture.nativeElement.querySelector('button[type="submit"]').getAttribute('aria-describedby')).toBeNull();
  });

  it('keeps a reviewed AI line reviewed after the user edits its price or quantity', () => {
    component.form.get('clienteId')?.setValue(7);
    component.cargarBorradorIa(draft());
    component.confirmarSugerenciaIa(0);
    component.manualItems.at(0).patchValue({ precioUnitario: 50, cantidad: 2.5 });
    component.actualizarPrecioIa(0);
    component.actualizarCantidadIa(0);

    expect(component.manualItems.at(0).get('iaRevisada')?.value).toBe(true);
    expect(component.motivosBloqueoCrear()).toEqual([]);
  });

  it('requires fresh confirmation for regenerated AI lines instead of transferring an old review', () => {
    component.form.get('clienteId')?.setValue(7);
    component.cargarBorradorIa(draft());
    component.confirmarSugerenciaIa(0);
    const previousId = component.manualItems.at(0).get('iaLineId')?.value;

    component.cargarBorradorIa(draft());

    expect(component.manualItems.at(0).get('iaLineId')?.value).not.toBe(previousId);
    expect(component.manualItems.at(0).get('iaRevisada')?.value).toBe(false);
    expect(component.motivosBloqueoCrear()).toContain('Confirma 1 sugerencias de IA.');
  });

  it('allows an approximate AI price after individual confirmation when price and quantity are valid', () => {
    component.form.get('clienteId')?.setValue(7);
    const approximate = {
      ...draft(),
      items: [{ ...draft().items[0], precioAproximado: true }],
    };
    component.cargarBorradorIa(approximate);
    component.confirmarSugerenciaIa(0);

    expect(component.pendientesIa()).toEqual({ precio: 0, cantidad: 0, revision: 0 });
    expect(component.motivosBloqueoCrear()).toEqual([]);
  });

  it.each(['2,5', '2.5'])('accepts quantity %s in a complete line', (quantity) => {
    component.form.get('clienteId')?.setValue(7);
    component.cargarBorradorIa(draft());
    component.manualItems.at(0).patchValue({ cantidad: quantity });
    component.actualizarCantidadIa(0);
    component.confirmarSugerenciaIa(0);

    expect(component.motivosBloqueoCrear()).toEqual([]);
  });

  it('reports that a quick-entered customer must be created before saving', () => {
    component.clienteModo = 'nuevo';
    component.nombreClienteNuevo = 'Cliente nuevo';
    component.cargarBorradorIa(draft());
    component.confirmarSugerenciaIa(0);

    expect(component.motivosBloqueoCrear()).toContain('Falta seleccionar o crear el cliente.');
    expect(component.botonCrearDeshabilitado()).toBe(true);
  });

  it('points to the specific invalid quantity when the standard form button is disabled', () => {
    component.form.get('clienteId')?.setValue(7);
    component.cargarBorradorIa(draft());
    component.confirmarSugerenciaIa(0);
    component.manualItems.at(0).patchValue({ cantidad: 0 });
    component.actualizarCantidadIa(0);
    fixture.detectChanges();

    expect(component.motivosBloqueoCrear()).toContain('Cantidad no válida en la partida 1.');
    const button = fixture.nativeElement.querySelector('button[type="submit"]');
    expect(button.disabled).toBe(true);
    expect(button.getAttribute('aria-describedby')).toBe('budget-submit-block-reasons');
    expect(fixture.nativeElement.querySelector('#budget-submit-block-reasons').textContent)
      .toContain('Cantidad no válida en la partida 1.');
  });

  it('explains permission and in-flight save blockers', () => {
    component.form.get('clienteId')?.setValue(7);
    component.cargarBorradorIa(draft());
    component.confirmarSugerenciaIa(0);
    vi.spyOn(component.auth, 'canMutate').mockReturnValue(false);
    expect(component.motivosBloqueoCrear()).toContain('Sin permiso');

    vi.spyOn(component.auth, 'canMutate').mockReturnValue(true);
    component.saving = true;
    expect(component.motivosBloqueoCrear()).toContain('Guardando');
  });

  it('explains a pending FormGroup validation instead of leaving the button unexplained', () => {
    component.form.get('clienteId')?.setValue(7);
    component.cargarBorradorIa(draft());
    component.confirmarSugerenciaIa(0);
    component.form.markAsPending();

    expect(component.botonCrearDeshabilitado()).toBe(true);
    expect(component.motivosBloqueoCrear()).toContain('Validando');
  });

  it('does not block on collapsed defaults or a zero-price manual line', () => {
    component.isEdit = true;
    component.id = 19;
    component.form.patchValue({
      clienteId: 7, estado: 'Aceptado', ivaHabilitado: true,
      descuentoGlobalPorcentaje: 0, descuentoGlobalFijo: 0,
      condiciones: { condicionesActivas: [], notaAdicional: '' },
    });
    component.anticipoCargando = true;
    component.addManualTaskItem();
    component.manualItems.at(0).patchValue({
      tareaManual: 'Trabajo manual gratuito', cantidad: 1, precioUnitario: 0,
    });

    expect(component.motivosBloqueoCrear()).toEqual([]);
    expect(component.botonCrearDeshabilitado()).toBe(false);
  });

  it.each([
    ['sent', { enviadoAt: '2026-10-01T10:00:00Z', estado: 'Pendiente' }, 'Este presupuesto ya se envió'],
    ['not pending', { enviadoAt: null, estado: 'Aceptado' }, 'Este presupuesto ya no está pendiente'],
  ] as const)('warns before generating over an existing %s estimate', (_label, details, warning) => {
    component.isEdit = true;
    component.form.patchValue({ estado: details.estado });
    component.presupuestoActual = {
      id: 1, clienteId: 7, clienteNombre: 'Cliente', fechaCreacion: '', subtotal: 0, iva: 0, total: 0,
      ivaHabilitado: true, estado: details.estado, items: [], enviadoAt: details.enviadoAt,
    } satisfies Presupuesto;
    fixture.detectChanges();

    expect(component.avisoPresupuestoExistenteIa()).toContain(warning);
    expect(fixture.nativeElement.querySelector('.overwrite-warning')?.textContent).toContain(warning);
  });

  it('uses the shared safe-suggestion rules to offer explicit bulk confirmation', () => {
    const current = draft();
    component.materiales = [{ id: 3, nombre: 'Pintura', precioUnitario: 45, unidadMedida: 'ud' }];
    component.cargarBorradorIa({
      ...current,
      items: [{
        ...current.items[0],
        materialId: 3,
        tareaManual: 'Pintura',
        confianza: 'alta',
      }],
    });

    expect(component.cantidadSegurasPorConfirmar()).toBe(1);
    component.abrirConfirmacionSeguras();
    expect(component.confirmacionSegurasAbierta).toBe(true);
    expect(component.resumenConfirmacionSeguras[0].description).toBe('Pintura');
  });

  it('renders translated AI help and its privacy note in the standard form', () => {
    expect(fixture.nativeElement.textContent).toContain('Cómo funciona Crear con IA');
    expect(fixture.nativeElement.querySelector('.hint-note')?.textContent).toContain('No incluyas datos personales');
  });

  it('shows a summary of unresolved AI review work', () => {
    component.cargarBorradorIa(draft());
    expect(component.pendientesIa().revision).toBe(1);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Pendientes 0 precio, 0 cantidad, 1 revisión');
  });
});

function draft(): PresupuestoIaBorradorResponse {
  return {
    clienteId: null,
    clienteNombre: null,
    clienteTelefono: null,
    transcripcion: 'Texto privado',
    items: [{
      materialId: null, materialNombre: null, tareaManual: 'Pintura', cantidad: 1,
      precioUnitario: 45, unidad: 'ud', aplicaIva: true, descuentoPorcentaje: 0, descuentoFijo: 0,
      visiblePdf: true, confianza: 'alta', faltaPrecio: false, cantidadDudosa: false,
      precioDictado: 45, precioTipo: 'unitario', precioAproximado: false, precioCatalogo: null,
      precioOrigen: 'dictado',
    }],
    notaAdicional: null,
  };
}
