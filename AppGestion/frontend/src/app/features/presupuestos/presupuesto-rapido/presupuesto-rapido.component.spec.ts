import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { PresupuestoRapidoComponent } from './presupuesto-rapido.component';
import { PresupuestoService } from '../../../core/services/presupuesto.service';
import { ClienteService } from '../../../core/services/cliente.service';
import { MaterialService } from '../../../core/services/material.service';
import { AuthService } from '../../../core/auth/auth.service';
import { MatSnackBar } from '@angular/material/snack-bar';

describe('PresupuestoRapidoComponent', () => {
  let fixture: ComponentFixture<PresupuestoRapidoComponent>;
  let component: PresupuestoRapidoComponent;
  const cliente = { id: 7, nombre: 'Cliente de prueba' };
  const material = { id: 3, nombre: 'Pintura blanca', precioUnitario: 10, unidadMedida: 'l' };
  const presupuestoService = { create: vi.fn(), downloadPdf: vi.fn() };
  const materialService = {
    getAll: vi.fn(),
    getTopUsados: vi.fn(),
    create: vi.fn(),
  };
  const authToken = signal<string | null>('test-token');
  const authUser = signal<{ id: number } | null>({ id: 42 });
  const authService = {
    canMutate: () => true,
    token: authToken,
    user: authUser,
    refreshUser: vi.fn(() => of({ id: 42 })),
  };
  const snackBar = { open: vi.fn() };

  beforeEach(async () => {
    window.localStorage.clear();
    vi.clearAllMocks();
    authToken.set('test-token');
    authUser.set({ id: 42 });
    presupuestoService.create.mockReturnValue(of({ id: 22, clienteId: 7, items: [], total: 24.2 }));
    materialService.getAll.mockReturnValue(of([material]));
    materialService.getTopUsados.mockReturnValue(of([material]));
    materialService.create.mockReturnValue(of({ ...material, id: 99 }));

    await TestBed.configureTestingModule({
      imports: [PresupuestoRapidoComponent, HttpClientTestingModule, NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideRouter([]),
        { provide: PresupuestoService, useValue: presupuestoService },
        { provide: ClienteService, useValue: { getAll: () => of([cliente]), createProvisional: vi.fn() } },
        { provide: MaterialService, useValue: materialService },
        { provide: AuthService, useValue: authService },
        { provide: MatSnackBar, useValue: snackBar },
      ],
    }).compileComponents();

    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('es', {
      snack: { budgetCreated: 'Presupuesto creado', budgetCreateFail: 'Error' },
      common: { close: 'Cerrar' },
      budQuick: {},
    });
    translate.setFallbackLang('es');
    translate.use('es');

    fixture = TestBed.createComponent(PresupuestoRapidoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => vi.restoreAllMocks());

  it('loads the most used materials and the full catalogue for search', () => {
    expect(component.materialesTop).toEqual([material]);
    expect(component.catalogoFiltrado()).toEqual([material]);
    component.busquedaMaterial = 'blanca';
    expect(component.catalogoFiltrado()).toEqual([material]);
    component.busquedaMaterial = 'inexistente';
    expect(component.catalogoFiltrado()).toEqual([]);
  });

  it('adds and removes material and free task lines', () => {
    expect(component.totalLineCount()).toBe(1);
    component.addMaterialLine();
    component.addManualLine();
    expect(component.totalLineCount()).toBe(3);
    component.removeManualLine(0);
    component.removeMaterialLine(1);
    expect(component.totalLineCount()).toBe(1);
  });

  it('updates the total using the shared cost utility and 21 percent VAT', () => {
    component.form.controls.clienteId.setValue(cliente.id);
    component.materialItems.at(0).patchValue({ materialId: material.id, cantidad: 2, precioUnitario: 10 });
    expect(component.totalPreview()).toBeCloseTo(24.2);
    component.form.controls.ivaHabilitado.setValue(false);
    expect(component.totalPreview()).toBe(20);
  });

  it('parses comma and point decimals without NaN or shifted values', () => {
    component.form.controls.clienteId.setValue(cliente.id);
    component.materialItems.at(0).patchValue({ materialId: material.id, cantidad: '12,5', precioUnitario: '4.25' });
    expect(component.totalPreview()).toBeCloseTo(12.5 * 4.25 * 1.21);
    component.crearYEnviar();
    expect(presupuestoService.create).toHaveBeenCalledWith(expect.objectContaining({
      items: [expect.objectContaining({ cantidad: 12.5, precioUnitario: 4.25 })],
    }));
    const item = presupuestoService.create.mock.calls.at(-1)![0].items[0];
    expect(Number.isNaN(item.cantidad)).toBe(false);
    expect(Number.isNaN(item.precioUnitario)).toBe(false);
  });

  it('uses decimal input mode for quantities and unit prices', () => {
    const inputs = fixture.nativeElement.querySelectorAll('[formControlName="cantidad"], [formControlName="precioUnitario"]') as NodeListOf<HTMLInputElement>;
    expect(inputs.length).toBeGreaterThan(0);
    for (const input of Array.from(inputs)) {
      expect(input.getAttribute('inputmode')).toBe('decimal');
      expect(input.type).toBe('text');
    }
  });

  it('does not allow invalid decimal values to create or display NaN totals', () => {
    component.form.controls.clienteId.setValue(cliente.id);
    component.materialItems.at(0).patchValue({ materialId: material.id, cantidad: '1,2,3', precioUnitario: 'NaN' });
    expect(Number.isFinite(component.totalPreview())).toBe(true);
    expect(component.puedeCrear()).toBe(false);
    component.crearYEnviar();
    expect(presupuestoService.create).not.toHaveBeenCalled();
  });

  it('restores a local draft and clears it after the estimate is created', () => {
    const key = 'presupuesto_rapido_borrador_v2_42';
    window.localStorage.setItem(key, JSON.stringify({
      version: 2,
      ownerId: 42,
      savedAt: Date.now(),
      data: {
        clienteId: cliente.id,
        materialItems: [{ materialId: material.id, descripcion: material.nombre, cantidad: 2, precioUnitario: 10 }],
        manualItems: [],
        ivaHabilitado: true,
        notaAdicional: 'Trabajo en cocina',
      },
    }));
    component['restaurarBorrador']();
    expect(component.form.controls.clienteId.value).toBe(cliente.id);
    expect(component.materialItems.at(0).get('materialId')?.value).toBe(material.id);
    expect(component.form.controls.notaAdicional.value).toBe('Trabajo en cocina');

    component.crearYEnviar();
    expect(presupuestoService.create).toHaveBeenCalledOnce();
    expect(window.localStorage.getItem(key)).toBeNull();
    expect(component.presupuestoCreado?.id).toBe(22);
  });

  it('scopes the local draft key to the authenticated user id', () => {
    component.form.controls.clienteId.setValue(cliente.id);
    component['guardarBorrador']();
    expect(window.localStorage.getItem('presupuesto_rapido_borrador_v2_42')).not.toBeNull();
    expect(window.localStorage.getItem('presupuesto_rapido_borrador_v2_7')).toBeNull();
    expect(JSON.parse(window.localStorage.getItem('presupuesto_rapido_borrador_v2_42')!).ownerId).toBe(42);
  });

  it('does not read or remove a different user’s scoped draft', () => {
    const otherKey = 'presupuesto_rapido_borrador_v2_99';
    window.localStorage.setItem(otherKey, JSON.stringify({ version: 2, ownerId: 99, savedAt: Date.now(), data: {} }));
    component['restaurarBorrador']();
    expect(window.localStorage.getItem(otherKey)).not.toBeNull();
    expect(component.form.controls.clienteId.value).toBeNull();
  });

  it('removes the current user draft when the authenticated session ends', async () => {
    component['guardarBorrador']();
    expect(window.localStorage.getItem('presupuesto_rapido_borrador_v2_42')).not.toBeNull();
    authToken.set(null);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(window.localStorage.getItem('presupuesto_rapido_borrador_v2_42')).toBeNull();
  });

  it.each([
    ['expired', { version: 2, ownerId: 42, savedAt: Date.now() - 8 * 24 * 60 * 60 * 1000, data: {} }],
    ['another version', { version: 1, ownerId: 42, savedAt: Date.now(), data: {} }],
    ['another owner', { version: 2, ownerId: 99, savedAt: Date.now(), data: {} }],
  ])('discards a draft that is %s', (_reason, value) => {
    const key = 'presupuesto_rapido_borrador_v2_42';
    window.localStorage.setItem(key, JSON.stringify(value));
    expect(() => component['restaurarBorrador']()).not.toThrow();
    expect(window.localStorage.getItem(key)).toBeNull();
  });

  it('discards corrupt local JSON without surfacing an error', () => {
    const key = 'presupuesto_rapido_borrador_v2_42';
    window.localStorage.setItem(key, '{broken');
    expect(() => component['restaurarBorrador']()).not.toThrow();
    expect(window.localStorage.getItem(key)).toBeNull();
  });

  it('saves a free line to the catalogue and uses its returned material id', () => {
    component.form.controls.clienteId.setValue(cliente.id);
    component.addManualLine();
    component.manualItems.at(0).patchValue({ tareaManual: 'Retirada de escombros', unidadMedida: 'viaje', cantidad: 1, precioUnitario: 35, guardarEnCatalogo: true });

    component.crearYEnviar();

    expect(materialService.create).toHaveBeenCalledWith({ nombre: 'Retirada de escombros', precioUnitario: 35, unidadMedida: 'viaje' });
    expect(presupuestoService.create).toHaveBeenCalledWith(expect.objectContaining({
      clienteId: cliente.id,
      items: [expect.objectContaining({ materialId: 99, cantidad: 1, precioUnitario: 35 })],
    }));
  });

  it('reuses an existing same-name catalogue material instead of creating a duplicate', () => {
    component.form.controls.clienteId.setValue(cliente.id);
    component.addManualLine();
    component.manualItems.at(0).patchValue({ tareaManual: '  PINTURA   BLANCA ', cantidad: 1, precioUnitario: 35, guardarEnCatalogo: true });
    component.crearYEnviar();
    expect(materialService.create).not.toHaveBeenCalled();
    expect(presupuestoService.create).toHaveBeenCalledWith(expect.objectContaining({
      items: [expect.objectContaining({ materialId: material.id, precioUnitario: 35 })],
    }));
  });

  it('asks for explicit confirmation before saving a zero-price material', () => {
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false);
    component.form.controls.clienteId.setValue(cliente.id);
    component.addManualLine();
    component.manualItems.at(0).patchValue({ tareaManual: 'Retirada gratis', cantidad: 1, precioUnitario: 0, guardarEnCatalogo: true });
    component.crearYEnviar();
    expect(confirm).toHaveBeenCalledOnce();
    expect(materialService.create).not.toHaveBeenCalled();
    expect(presupuestoService.create).toHaveBeenCalledWith(expect.objectContaining({
      items: [expect.objectContaining({ tareaManual: 'Retirada gratis', precioUnitario: 0 })],
    }));
  });

  it('blocks creation without a client or without at least one described line', () => {
    expect(component.puedeCrear()).toBe(false);
    component.form.controls.clienteId.setValue(cliente.id);
    expect(component.puedeCrear()).toBe(false);
    component.materialItems.at(0).patchValue({ materialId: material.id });
    expect(component.puedeCrear()).toBe(true);
  });

  it('does not render photo or signature controls', () => {
    expect(fixture.nativeElement.textContent).not.toContain('Adjuntar foto');
    expect(fixture.nativeElement.textContent).not.toContain('Firma del cliente');
  });
});
