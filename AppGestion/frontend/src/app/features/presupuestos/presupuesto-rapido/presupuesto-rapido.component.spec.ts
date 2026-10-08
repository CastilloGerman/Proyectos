import { ComponentFixture, TestBed } from '@angular/core/testing';
import { isDevMode, signal } from '@angular/core';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { of, Subject, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { PresupuestoRapidoComponent } from './presupuesto-rapido.component';
import { PresupuestoService } from '../../../core/services/presupuesto.service';
import { ClienteService } from '../../../core/services/cliente.service';
import { MaterialService } from '../../../core/services/material.service';
import { AuthService } from '../../../core/auth/auth.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PresupuestoIaService } from '../../../core/services/presupuesto-ia.service';
import { PresupuestoIaBorradorResponse, PresupuestoIaRequestError } from '../../../core/models/presupuesto-ia.model';

describe('PresupuestoRapidoComponent', () => {
  let fixture: ComponentFixture<PresupuestoRapidoComponent>;
  let component: PresupuestoRapidoComponent;
  const cliente = { id: 7, nombre: 'Cliente de prueba' };
  const material = { id: 3, nombre: 'Pintura blanca', precioUnitario: 10, unidadMedida: 'l' };
  const presupuestoService = { create: vi.fn(), downloadPdf: vi.fn() };
  const presupuestoIaService = { generarBorrador: vi.fn() };
  const clienteService = {
    getAll: vi.fn(() => of([cliente])),
    createProvisional: vi.fn(),
    create: vi.fn(() => of({ ...cliente, telefono: '600111222' })),
  };
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
    presupuestoIaService.generarBorrador.mockReturnValue(of(iaDraft()));
    clienteService.create.mockReturnValue(of({ ...cliente, telefono: '600111222' }));

    await TestBed.configureTestingModule({
      imports: [PresupuestoRapidoComponent, HttpClientTestingModule, NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideRouter([]),
        { provide: PresupuestoService, useValue: presupuestoService },
        { provide: ClienteService, useValue: clienteService },
        { provide: MaterialService, useValue: materialService },
        { provide: PresupuestoIaService, useValue: presupuestoIaService },
        { provide: AuthService, useValue: authService },
        { provide: MatSnackBar, useValue: snackBar },
      ],
    }).compileComponents();

    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('es', {
      snack: { budgetCreated: 'Presupuesto creado', budgetCreateFail: 'Error' },
      common: { close: 'Cerrar' },
      budQuick: { ai: {
        progress: 'Organizando…', draftReady: 'Borrador listo', disabled: 'IA desactivada',
        quotaDaily: 'Límite diario. Inténtalo mañana.', quotaHourly: 'Límite por hora.',
        quotaAttempts: 'Límite de intentos.', quotaProvider: 'Cuota del proveedor.',
        providerError: 'Error temporal', tooLong: 'Texto demasiado largo', forbidden: 'Sin permiso',
        unauthorized: 'Sesión caducada', invalidRequest: 'Solicitud no válida',
        invalidResponse: 'Demasiadas partidas o datos incompletos. Divide la descripción y revisa cada partida.',
        genericError: 'Error',
        confirmOverwrite: '¿Reemplazar partidas?', associatedMaterial: 'Material asociado',
        suggested: 'Sugerida por IA', reviewed: 'Revisada', missingPrice: 'Falta precio',
        missingQuantity: 'Falta cantidad', pendingSummary: '{{price}} sin precio; {{quantity}} sin cantidad; {{review}} por revisar',
        clientSuggestion: 'Cliente sugerido', useClient: 'Usar', createProvisional: 'Crear provisional',
        lowConfidence: 'Revisar confianza', changeMaterial: 'Cambiar material', freeItem: 'Partida libre',
        confirmSuggestion: 'Confirmar',
        confirmSafe: 'Confirmar las {{count}} seguras',
        priceTip: 'Di el precio y si es unitario o total',
        dictatedPrice: 'Precio dictado',
        approximatePrice: 'Precio aproximado',
        catalogPrice: 'En tu catálogo',
        priceIncludedInPreviousLine: 'Precio incluido en la línea anterior',
        submitBlock: {
          permission: 'Sin permiso', saving: 'Guardando', alreadyCreated: 'Ya creado',
          generating: 'Espera a que termine la generación con IA.', validationPending: 'Validando',
          customer: 'Select or create a customer.', line: 'Add a line',
          review: 'Confirm {{count}} AI suggestions.', price: 'Missing price {{count}}',
          quantity: 'Missing quantity {{count}}', invalidQuantity: 'Invalid quantity in line {{line}}.',
          invalidPrice: 'Invalid price in line {{line}}.', invalidForm: 'Invalid form',
        },
        formPending: 'Pendientes {{count}}',
        formReviewRequired: 'Revisa cada partida',
        safeDialogTitle: 'Revisa las sugerencias seguras',
        safeDialogDescription: 'Comprueba descripción, material y precio antes de confirmar.',
        confirmAllSafe: 'Confirmar todas',
        cancelSafe: 'Cancelar',
      } },
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
    const firstLineId = component.materialItems.at(0).get('lineId')?.value;
    component.addMaterialLine();
    component.addManualLine();
    expect(component.totalLineCount()).toBe(3);
    expect(component.materialItems.at(0).get('lineId')?.value).toBe(firstLineId);
    const ids = [
      ...component.materialItems.controls,
      ...component.manualItems.controls,
    ].map((line) => line.get('lineId')?.value);
    expect(new Set(ids).size).toBe(ids.length);
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

  it('persists AI line data and review flags in the local draft but never the dictated text', () => {
    component.textoObraIa = 'Texto privado dictado';
    component.generarConIa();
    component.manualItems.at(1).get('iaRevisada')?.setValue(true);
    component['guardarBorrador']();

    const saved = JSON.parse(window.localStorage.getItem('presupuesto_rapido_borrador_v2_42')!);
    expect(JSON.stringify(saved)).not.toContain('Texto privado dictado');
    expect(saved.data.manualItems[1]).toMatchObject({
      tareaManual: 'Pintar pared',
      materialId: 3,
      materialNombre: 'Pintura blanca',
      iaSugerida: true,
      iaRevisada: true,
      confianza: 'media',
      faltaPrecio: false,
      cantidadDudosa: false,
    });
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

  it('loads the AI response as an editable draft and shows its associated material name', () => {
    component.textoObraIa = 'Alicatar baño y colocar ducha';
    component.generarConIa();
    fixture.detectChanges();

    expect(component.manualItems.length).toBe(2);
    expect(component.manualItems.at(0).get('iaSugerida')?.value).toBe(true);
    expect(component.manualItems.at(0).get('faltaPrecio')?.value).toBe(true);
    expect(component.manualItems.at(0).get('precioUnitario')?.value).toBe(0);
    expect(component.manualItems.at(1).get('materialNombre')?.value).toBe('Pintura blanca');
    expect(component.manualItems.at(1).get('precioUnitario')?.value).toBe(10);
    expect(component.manualItems.at(1).get('faltaPrecio')?.value).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('Material asociado');
    expect(fixture.nativeElement.textContent).toContain('Pintura blanca');
    expect(presupuestoService.create).not.toHaveBeenCalled();
  });

  it('enables create for a complete customer-selected draft after confirming every AI line', () => {
    component.form.controls.clienteId.setValue(cliente.id);
    component.cargarBorradorIa({
      ...iaDraft(),
      items: [{
        ...iaDraft().items[1], tareaManual: 'Pintar habitación', confianza: 'alta',
        cantidad: 2, precioUnitario: 10, faltaPrecio: false, cantidadDudosa: false,
      }],
    });
    component.confirmarSugerenciaIa(0);
    fixture.detectChanges();

    expect(component.puedeCrear()).toBe(true);
    expect(fixture.nativeElement.querySelector('.submit-btn').disabled).toBe(false);
    expect(fixture.nativeElement.querySelector('.submit-btn').getAttribute('aria-describedby')).toBeNull();
  });

  it('keeps confirmation after editing a reviewed AI price or quantity', () => {
    component.form.controls.clienteId.setValue(cliente.id);
    component.cargarBorradorIa({
      ...iaDraft(),
      items: [{ ...iaDraft().items[1], confianza: 'alta', faltaPrecio: false, cantidadDudosa: false }],
    });
    component.confirmarSugerenciaIa(0);
    component.manualItems.at(0).patchValue({ precioUnitario: '12,50', cantidad: '2,5' });
    component.actualizarPrecioIa(0);
    component.actualizarCantidadIa(0);

    expect(component.manualItems.at(0).get('iaRevisada')?.value).toBe(true);
    expect(component.puedeCrear()).toBe(true);
  });

  it('requires confirmation for regenerated AI lines and does not reuse an old line review', () => {
    component.form.controls.clienteId.setValue(cliente.id);
    const validDraft: PresupuestoIaBorradorResponse = {
      ...iaDraft(),
      items: [{ ...iaDraft().items[1], confianza: 'alta', faltaPrecio: false, cantidadDudosa: false }],
    };
    component.cargarBorradorIa(validDraft);
    component.confirmarSugerenciaIa(0);
    const oldLineId = component.manualItems.at(0).get('lineId')?.value;

    component.cargarBorradorIa(validDraft);

    expect(component.manualItems.at(0).get('lineId')?.value).not.toBe(oldLineId);
    expect(component.manualItems.at(0).get('iaRevisada')?.value).toBe(false);
    expect(component.motivosBloqueoCrear()).toContain('Confirm 1 AI suggestions.');
  });

  it('allows an individually confirmed approximate dictated price', () => {
    component.form.controls.clienteId.setValue(cliente.id);
    component.cargarBorradorIa({
      ...iaDraft(),
      items: [{
        ...iaDraft().items[1], confianza: 'alta', precioOrigen: 'dictado',
        precioAproximado: true, faltaPrecio: false, cantidadDudosa: false,
      }],
    });
    component.confirmarSugerenciaIa(0);

    expect(component.puedeCrear()).toBe(true);
    expect(component.pendientesIa()).toEqual({ precio: 0, cantidad: 0, revision: 0 });
  });

  it.each(['2,5', '2.5'])('accepts quantity %s with point/comma decimal parsing', (quantity) => {
    component.form.controls.clienteId.setValue(cliente.id);
    component.addManualLine();
    component.manualItems.at(0).patchValue({
      tareaManual: 'Trabajo manual', cantidad: quantity, precioUnitario: '10,25',
    });
    component.actualizarCantidadIa(0);

    expect(component.puedeCrear()).toBe(true);
  });

  it('explains that typing a new customer name is not enough until the customer is created', () => {
    component.clienteModo = 'nuevo';
    component.nombreClienteNuevo = 'Cliente nuevo';
    component.addManualLine();
    component.manualItems.at(0).patchValue({
      tareaManual: 'Trabajo', cantidad: 1, precioUnitario: 10,
    });

    expect(component.puedeCrear()).toBe(false);
    expect(component.motivosBloqueoCrear()).toContain('Select or create a customer.');

    component.clienteModo = 'existente';
    component.form.controls.clienteId.setValue(cliente.id);
    expect(component.motivosBloqueoCrear()).not.toContain('Select or create a customer.');
    expect(component.puedeCrear()).toBe(true);
  });

  it('does not block on IVA defaults or an intentional zero-price manual line', () => {
    component.form.controls.clienteId.setValue(cliente.id);
    component.addManualLine();
    component.manualItems.at(0).patchValue({
      tareaManual: 'Trabajo gratuito', cantidad: 1, precioUnitario: 0,
    });

    expect(component.form.controls.ivaHabilitado.value).toBe(true);
    expect(component.puedeCrear()).toBe(true);
    expect(component.motivosBloqueoCrear()).toEqual([]);
  });

  it('identifies invalid active-line quantities beside the disabled submit button', () => {
    component.form.controls.clienteId.setValue(cliente.id);
    component.addManualLine();
    component.manualItems.at(0).patchValue({
      tareaManual: 'Trabajo', cantidad: '0', precioUnitario: 10,
    });
    fixture.detectChanges();

    expect(component.motivosBloqueoCrear()).toContain('Invalid quantity in line 1.');
    const button = fixture.nativeElement.querySelector('.submit-btn');
    expect(button.disabled).toBe(true);
    expect(button.getAttribute('aria-describedby')).toBe('quick-submit-block-reasons');
    expect(fixture.nativeElement.querySelector('#quick-submit-block-reasons').textContent)
      .toContain('Invalid quantity in line 1.');
  });

  it('explains permission and in-flight submission blockers', () => {
    component.form.controls.clienteId.setValue(cliente.id);
    component.addManualLine();
    component.manualItems.at(0).patchValue({
      tareaManual: 'Trabajo', cantidad: 1, precioUnitario: 10,
    });
    vi.spyOn(component.auth, 'canMutate').mockReturnValue(false);
    expect(component.motivosBloqueoCrear()).toContain('Sin permiso');

    vi.spyOn(component.auth, 'canMutate').mockReturnValue(true);
    component.loading = true;
    expect(component.motivosBloqueoCrear()).toContain('Guardando');
  });

  it('explains that AI generation is still in progress beside the disabled button', () => {
    component.form.controls.clienteId.setValue(cliente.id);
    component.addManualLine();
    component.manualItems.at(0).patchValue({
      tareaManual: 'Trabajo', cantidad: 1, precioUnitario: 10,
    });
    component.iaLoading = true;
    fixture.detectChanges();

    expect(component.puedeCrear()).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('Espera a que termine la generación con IA.');
  });

  it('logs only the permitted per-line AI review fields in development mode', () => {
    const debug = vi.spyOn(console, 'debug').mockImplementation(() => undefined);
    presupuestoIaService.generarBorrador.mockReturnValue(of(iaDraft({
      clienteNombre: 'No registrar nombre',
      clienteTelefono: 'No registrar teléfono',
      transcripcion: 'No registrar texto completo',
    })));
    component.textoObraIa = 'Texto completo privado';

    component.generarConIa();

    if (isDevMode()) {
      expect(debug).toHaveBeenCalledOnce();
      const logged = JSON.stringify(debug.mock.calls);
      expect(logged).toContain('materialId');
      expect(logged).toContain('confianza');
      expect(logged).not.toContain('Alicatar baño');
      expect(logged).not.toContain('No registrar nombre');
      expect(logged).not.toContain('No registrar teléfono');
      expect(logged).not.toContain('No registrar texto completo');
      expect(logged).not.toContain('Texto completo privado');
    } else {
      expect(debug).not.toHaveBeenCalled();
    }
  });

  it('shows dictated and approximate price labels with icons and a different catalogue reference', () => {
    presupuestoIaService.generarBorrador.mockReturnValue(of(iaDraft({
      items: [{
        materialId: material.id, materialNombre: material.nombre, tareaManual: 'Pintar pared', cantidad: 1,
        precioUnitario: 25, unidad: 'l', aplicaIva: true, descuentoPorcentaje: 0, descuentoFijo: 0,
        visiblePdf: true, confianza: 'alta', faltaPrecio: false, cantidadDudosa: false,
        precioDictado: 25, precioTipo: 'unitario', precioAproximado: true, precioCatalogo: 10, precioOrigen: 'dictado',
      }],
    })));

    component.textoObraIa = 'Pintura a 25 euros aproximadamente';
    component.generarConIa();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Precio aproximado');
    expect(fixture.nativeElement.textContent).toContain('En tu catálogo');
    expect(fixture.nativeElement.textContent).toContain('10.00');
    expect(fixture.nativeElement.querySelector('.price-badge mat-icon')?.textContent.trim()).toBe('functions');

    component.manualItems.at(0).patchValue({ precioAproximado: false });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Precio dictado');
    expect(fixture.nativeElement.querySelector('.price-badge mat-icon')?.textContent.trim()).toBe('record_voice_over');
  });

  it('shows a helpful localized message when AI returns too many or malformed lines', () => {
    presupuestoIaService.generarBorrador.mockReturnValue(throwError(() =>
      new PresupuestoIaRequestError('invalid-response', 502)));
    component.textoObraIa = 'Descripción extensa de la obra';

    component.generarConIa();
    fixture.detectChanges();

    expect(component.iaErrorMessage).toContain('Demasiadas partidas o datos incompletos');
    expect(component.iaErrorMessage).not.toBe('Error');
    expect(fixture.nativeElement.textContent).toContain('revisa cada partida');
  });

  it('shows an explanatory note on a line covered by the previous total', () => {
    component.cargarBorradorIa({
      ...iaDraft(),
      items: [
        { ...iaDraft().items[0], precioUnitario: 70, precioDictado: 70, precioTipo: 'total', precioOrigen: 'dictado' },
        { ...iaDraft().items[0], tareaManual: 'Otro rollo de cable', precioUnitario: 0,
          precioIncluidoEnLineaAnterior: true, faltaPrecio: true },
      ],
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Precio incluido en la línea anterior');
  });

  it('keeps a missing AI quantity blank instead of showing the form default of one', () => {
    presupuestoIaService.generarBorrador.mockReturnValue(of(iaDraft({
      items: [{
        materialId: null, materialNombre: null, tareaManual: 'Partida contable', cantidad: 1,
        precioUnitario: 20, unidad: 'ud', aplicaIva: true, descuentoPorcentaje: 0, descuentoFijo: 0,
        visiblePdf: true, confianza: 'alta', faltaPrecio: false, cantidadDudosa: true,
      }],
    })));

    component.textoObraIa = 'Trabajo con precio 20 euros';
    component.generarConIa();

    expect(component.manualItems.at(0).get('cantidad')?.value).toBeNull();
    expect(component.manualItems.at(0).get('cantidadDudosa')?.value).toBe(true);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[formArrayName="manualItems"] [formControlName="cantidad"]').value)
      .toBe('');
    expect(fixture.nativeElement.textContent).toContain('Falta cantidad');
  });

  it('keeps a dictated price when the user removes its catalogue association', () => {
    presupuestoIaService.generarBorrador.mockReturnValue(of(iaDraft({
      items: [{
        materialId: material.id, materialNombre: material.nombre, tareaManual: 'Pintar pared', cantidad: 1,
        precioUnitario: 25, unidad: 'l', aplicaIva: true, descuentoPorcentaje: 0, descuentoFijo: 0,
        visiblePdf: true, confianza: 'alta', faltaPrecio: false, cantidadDudosa: false,
        precioDictado: 25, precioTipo: 'unitario', precioAproximado: false, precioCatalogo: 10, precioOrigen: 'dictado',
      }],
    })));
    component.textoObraIa = 'Pintura a 25 euros cada bote';
    component.generarConIa();

    component.onIaMaterialChange(0, null);

    expect(component.manualItems.at(0).get('precioUnitario')?.value).toBe(25);
    expect(component.manualItems.at(0).get('precioOrigen')?.value).toBe('dictado');
    expect(component.manualItems.at(0).get('faltaPrecio')?.value).toBe(false);
    expect(component.manualItems.at(0).get('precioCatalogo')?.value).toBeNull();
  });

  it('blocks create and send until AI price and quantity flags are completed', () => {
    component.form.controls.clienteId.setValue(cliente.id);
    component.textoObraIa = 'Alicatar baño y pintar pared';
    component.generarConIa();
    fixture.detectChanges();
    expect(component.puedeCrear()).toBe(false);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.submit-btn').disabled).toBe(true);
    expect(component.pendientesIa()).toEqual({ precio: 1, cantidad: 1, revision: 2 });
    expect(fixture.nativeElement.textContent).toContain('1 sin precio; 1 sin cantidad; 2 por revisar');
    const fixedBar = fixture.nativeElement.querySelector('.submit-bar');
    expect(fixedBar.querySelector('.pending-ai-summary').textContent).toContain('sin precio');
    expect(fixedBar.querySelector('.submit-btn').disabled).toBe(true);

    component.manualItems.at(0).patchValue({ precioUnitario: '19,5', cantidad: '3' });
    component.actualizarPrecioIa(0);
    component.actualizarCantidadIa(0);
    expect(component.pendientesIa().revision).toBe(2);
    component.confirmarSugerenciaIa(0);
    component.confirmarSugerenciaIa(1);
    expect(component.pendientesIa()).toEqual({ precio: 0, cantidad: 0, revision: 0 });
    expect(component.puedeCrear()).toBe(true);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.submit-btn').disabled).toBe(false);
  });

  it('shows a per-line confirmation summary and confirms safe suggestions only after acceptance', async () => {
    component.textoObraIa = 'Pintar pared';
    component.generarConIa();
    component.manualItems.at(0).patchValue({
      materialId: material.id, materialNombre: material.nombre, confianza: 'alta',
      cantidad: 1, precioUnitario: 20, faltaPrecio: false, cantidadDudosa: false,
    });
    component.addManualLine();
    component.manualItems.at(2).patchValue({
      tareaManual: 'Trabajo sin material válido', materialId: 999, confianza: 'alta',
      cantidad: 1, precioUnitario: 20, iaSugerida: true, faltaPrecio: false, cantidadDudosa: false,
    });
    component.addManualLine();
    component.manualItems.at(3).patchValue({
      tareaManual: 'Trabajo marcado sin precio', materialId: material.id, confianza: 'alta',
      cantidad: 1, precioUnitario: 20, iaSugerida: true, faltaPrecio: true, cantidadDudosa: false,
    });

    fixture.detectChanges();
    expect(component.cantidadSegurasPorConfirmar()).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('Confirmar las 1 seguras');
    fixture.nativeElement.querySelector('.confirm-safe-btn').click();
    fixture.detectChanges();
    await new Promise((resolve) => setTimeout(resolve, 1));
    fixture.detectChanges();

    const dialog = fixture.nativeElement.querySelector('dialog[aria-modal="true"]');
    expect(dialog).not.toBeNull();
    expect(dialog.textContent).toContain('Alicatar baño');
    expect(dialog.textContent).toContain('Pintura blanca');
    expect(dialog.textContent).toContain('20.00');
    expect(dialog.textContent).toContain('Confirmar todas');
    expect(dialog.textContent).toContain('Cancelar');
    expect(document.activeElement).toBe(dialog);
    expect(component.manualItems.at(0).get('iaRevisada')?.value).toBe(false);
    expect(component.manualItems.at(1).get('iaRevisada')?.value).toBe(false);

    dialog.querySelector('.safe-dialog-button:last-child').click();
    fixture.detectChanges();

    expect(component.manualItems.at(0).get('iaRevisada')?.value).toBe(true);
    expect(component.manualItems.at(1).get('iaRevisada')?.value).toBe(false);
    expect(component.manualItems.at(2).get('iaRevisada')?.value).toBe(false);
    expect(component.manualItems.at(3).get('iaRevisada')?.value).toBe(false);
  });

  it('allows exact dictated prices in safe confirmation but excludes approximate prices', () => {
    component.textoObraIa = 'Pintar pared';
    component.generarConIa();
    component.manualItems.at(0).patchValue({
      tareaManual: 'Partida dictada', confianza: 'alta', cantidad: 1, precioUnitario: 25,
      precioOrigen: 'dictado', precioAproximado: true, faltaPrecio: false, cantidadDudosa: false,
    });
    expect(component.cantidadSegurasPorConfirmar()).toBe(0);

    component.manualItems.at(0).get('precioAproximado')?.setValue(false);
    expect(component.cantidadSegurasPorConfirmar()).toBe(1);
  });

  it('restores a flagged AI quantity as blank rather than the default value of one', () => {
    window.localStorage.setItem('presupuesto_rapido_borrador_v2_42', JSON.stringify({
      version: 2,
      ownerId: 42,
      savedAt: Date.now(),
      data: {
        clienteId: cliente.id,
        materialItems: [],
        manualItems: [{
          tareaManual: 'Un trabajo sin cantidad', cantidad: null, precioUnitario: 0,
          iaSugerida: true, cantidadDudosa: true, faltaPrecio: true,
        }],
        ivaHabilitado: true,
        notaAdicional: '',
      },
    }));

    component['restaurarBorrador']();

    expect(component.manualItems.at(0).get('cantidad')?.value).toBeNull();
    expect(component.manualItems.at(0).get('cantidadDudosa')?.value).toBe(true);
  });

  it('cancels the safe-suggestions summary without confirming any line', async () => {
    component.manualItems.clear();
    component.manualItems.push(component['createManualLine']());
    component.manualItems.at(0).patchValue({
      tareaManual: 'Instalar material', materialId: material.id, materialNombre: material.nombre,
      iaSugerida: true, iaRevisada: false, confianza: 'alta', cantidad: 1,
      precioUnitario: 10, faltaPrecio: false, cantidadDudosa: false,
    });
    fixture.detectChanges();

    fixture.nativeElement.querySelector('.confirm-safe-btn').click();
    fixture.detectChanges();
    await new Promise((resolve) => setTimeout(resolve, 1));
    fixture.detectChanges();
    fixture.nativeElement.querySelector('.safe-dialog-button:first-child').click();
    fixture.detectChanges();

    expect(component.confirmacionSegurasAbierta).toBe(false);
    expect(component.manualItems.at(0).get('iaRevisada')?.value).toBe(false);
  });

  it('lets the user remove an AI material association and keep the line as free text', () => {
    component.textoObraIa = 'Pintar pared';
    component.generarConIa();
    component.onIaMaterialChange(1, null);
    expect(component.manualItems.at(1).get('materialId')?.value).toBeNull();
    expect(component.manualItems.at(1).get('materialNombre')?.value).toBe('');
    expect(component.manualItems.at(1).get('precioUnitario')?.value).toBe(0);
    expect(component.manualItems.at(1).get('faltaPrecio')?.value).toBe(true);
    expect(component['buildItemsPayload']()).toContainEqual(expect.objectContaining({
      tareaManual: 'Pintar pared', precioUnitario: 0,
    }));
  });

  it('requires explicit per-line review even after editing the description or price', () => {
    component.form.controls.clienteId.setValue(cliente.id);
    component.textoObraIa = 'Pintar pared';
    component.generarConIa();
    component.manualItems.at(0).patchValue({ cantidad: 1, precioUnitario: 10 });
    component.actualizarCantidadIa(0);
    component.actualizarPrecioIa(0);
    component.manualItems.at(1).get('tareaManual')?.setValue('Pintar dormitorio');
    component.manualItems.at(1).get('precioUnitario')?.setValue(12);
    component.actualizarPrecioIa(1);

    expect(component.pendientesIa().revision).toBe(2);
    expect(component.puedeCrear()).toBe(false);

    component.confirmarSugerenciaIa(0);
    component.confirmarSugerenciaIa(1);
    expect(component.puedeCrear()).toBe(true);
  });

  it('asks before replacing manually reviewed AI lines after another generation', () => {
    component.textoObraIa = 'Pintar una habitación';
    component.generarConIa();
    component.manualItems.at(0).get('iaRevisada')?.setValue(true);
    component.form.controls.notaAdicional.setValue('Nota que no debe perderse');
    const oldLines = component.manualItems.getRawValue();
    component.textoObraIa = 'Pintar dos habitaciones';
    vi.spyOn(window, 'confirm').mockReturnValue(false);

    component.generarConIa();

    expect(window.confirm).toHaveBeenCalledOnce();
    expect(presupuestoIaService.generarBorrador).toHaveBeenCalledOnce();
    expect(component.manualItems.at(0).get('tareaManual')?.value).toBe('Alicatar baño');
    expect(component.textoObraIa).toBe('Pintar dos habitaciones');
    expect(component.manualItems.getRawValue()).toEqual(oldLines);
    expect(component.form.controls.notaAdicional.value).toBe('Nota que no debe perderse');
  });

  it('prevents a second AI request while the first request is pending', () => {
    const pending = new Subject<PresupuestoIaBorradorResponse>();
    presupuestoIaService.generarBorrador.mockReturnValue(pending);
    component.textoObraIa = 'Pintar la cocina';

    component.generarConIa();
    component.generarConIa();

    expect(presupuestoIaService.generarBorrador).toHaveBeenCalledOnce();
    pending.next(iaDraft());
    pending.complete();
  });

  it('disables AI after the backend reports that it is off', () => {
    presupuestoIaService.generarBorrador.mockReturnValue(throwError(() =>
      new PresupuestoIaRequestError('disabled', 503, 'El servicio de IA está desactivado')));
    component.textoObraIa = 'Pintar la cocina';

    component.generarConIa();
    fixture.detectChanges();

    expect(component.iaDesactivada).toBe(true);
    expect(component.iaLoading).toBe(false);
    expect(fixture.nativeElement.querySelector('.ai-generate').disabled).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('IA desactivada');
  });

  it('keeps manual budget creation available when AI is disabled', () => {
    presupuestoIaService.generarBorrador.mockReturnValue(throwError(() =>
      new PresupuestoIaRequestError('disabled', 503, 'El servicio de IA está desactivado')));
    component.textoObraIa = 'Pintar la cocina';
    component.generarConIa();
    component.form.controls.clienteId.setValue(cliente.id);
    component.materialItems.at(0).patchValue({ materialId: material.id, cantidad: 1, precioUnitario: 10 });

    expect(component.puedeCrear()).toBe(true);
    component.crearYEnviar();
    expect(presupuestoService.create).toHaveBeenCalledOnce();
  });

  it('shows distinct hourly and daily quota messages', () => {
    presupuestoIaService.generarBorrador.mockReturnValue(throwError(() =>
      new PresupuestoIaRequestError('quota-hourly', 429, 'límite por hora')));
    component.textoObraIa = 'Reparar una fuga';
    component.generarConIa();
    const hourlyMessage = component.iaErrorMessage;

    presupuestoIaService.generarBorrador.mockReturnValue(throwError(() =>
      new PresupuestoIaRequestError('quota-daily', 429, 'límite diario')));
    component.generarConIa();

    expect(hourlyMessage).toContain('por hora');
    expect(component.iaErrorMessage).toContain('mañana');
    expect(component.iaErrorMessage).not.toBe(hourlyMessage);
  });

  it('shows the daily quota state and renewal hint', () => {
    presupuestoIaService.generarBorrador.mockReturnValue(throwError(() =>
      new PresupuestoIaRequestError('quota-daily', 429, 'límite diario')));
    component.textoObraIa = 'Reparar una fuga';

    component.generarConIa();
    fixture.detectChanges();

    expect(component.iaErrorMessage).toContain('mañana');
    expect(fixture.nativeElement.querySelector('[role="alert"]')).not.toBeNull();
  });

  it('keeps the written text and existing lines after a provider error', () => {
    presupuestoIaService.generarBorrador.mockReturnValue(throwError(() =>
      new PresupuestoIaRequestError('provider', 503, 'error del proveedor')));
    component.textoObraIa = 'Reformar baño, seis metros';
    component.addManualLine();
    component.manualItems.at(0).patchValue({ tareaManual: 'Trabajo existente' });
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    component.generarConIa();

    expect(component.textoObraIa).toBe('Reformar baño, seis metros');
    expect(component.manualItems.at(0).get('tareaManual')?.value).toBe('Trabajo existente');
    expect(component.iaErrorMessage).toBe('Error temporal');
  });

  it('renders model text as text, not as HTML', () => {
    presupuestoIaService.generarBorrador.mockReturnValue(of(iaDraft({
      clienteNombre: '<script>alert(1)</script>',
      items: [{ ...iaDraft().items[0], tareaManual: '<img src=x onerror=alert(1)>' }],
      notaAdicional: '<svg onload=alert(1)>',
    })));
    component.textoObraIa = 'Trabajo';

    component.generarConIa();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('script, img, svg[onload]')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('<script>alert(1)</script>');
    expect(component.form.controls.notaAdicional.value).toBe('<svg onload=alert(1)>');
  });

  it('marks AI suggestions with readable text and an icon, not color alone', () => {
    component.textoObraIa = 'Pintar pared';
    component.generarConIa();
    fixture.detectChanges();

    const card = fixture.nativeElement.querySelector('.ai-review-card');
    expect(card.textContent).toContain('Sugerida por IA');
    expect(card.querySelector('.ai-label mat-icon')?.textContent).toContain('auto_awesome');
    component.confirmarSugerenciaIa(0);
    fixture.detectChanges();
    expect(card.textContent).toContain('Revisada');
    expect(card.textContent).not.toContain('pendiente de revisar');
  });

  it('requests focus on the status region after successful generation and after an error', () => {
    const statusElement = fixture.nativeElement.querySelector('#ia-status');
    const focus = vi.spyOn(statusElement, 'focus');
    vi.spyOn(window, 'setTimeout').mockImplementation(((handler: TimerHandler) => {
      if (typeof handler === 'function') handler();
      return 0;
    }) as typeof window.setTimeout);
    component.textoObraIa = 'Pintar pared';
    component.generarConIa();
    expect(focus).toHaveBeenCalledOnce();

    focus.mockClear();
    presupuestoIaService.generarBorrador.mockReturnValue(throwError(() =>
      new PresupuestoIaRequestError('provider', 503, 'error del proveedor')));
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    component.generarConIa();
    expect(focus).toHaveBeenCalledOnce();
  });

  it('announces AI status and errors accessibly', () => {
    component.textoObraIa = 'Pintar pared';
    component.generarConIa();
    fixture.detectChanges();
    const status = fixture.nativeElement.querySelector('#ia-status');
    expect(status.getAttribute('role')).toBe('status');
    expect(status.getAttribute('aria-live')).toBe('polite');

    presupuestoIaService.generarBorrador.mockReturnValue(throwError(() =>
      new PresupuestoIaRequestError('provider', 503, 'error del proveedor')));
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    component.generarConIa();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#ia-status').getAttribute('role')).toBe('alert');
  });

  it('keeps AI action targets at least 44 pixels high', () => {
    const buttons = fixture.nativeElement.querySelectorAll('.ai-generate, .confirm-ai-btn, .confirm-safe-btn');
    for (const button of Array.from(buttons) as HTMLButtonElement[]) {
      expect(button.offsetHeight || Number.parseInt(getComputedStyle(button).minHeight, 10)).toBeGreaterThanOrEqual(44);
    }
  });

  it('offers detected clients without assigning them automatically and preserves the phone when creating one', () => {
    presupuestoIaService.generarBorrador.mockReturnValue(of(iaDraft({ clienteNombre: 'Cliente detectado', clienteTelefono: '600111222' })));
    component.textoObraIa = 'Trabajo para cliente detectado';

    component.generarConIa();
    fixture.detectChanges();

    expect(component.form.controls.clienteId.value).toBeNull();
    expect(component.clienteSugerido?.telefono).toBe('600111222');
    component.crearClienteSugerido();
    expect(clienteService.create).toHaveBeenCalledWith({ nombre: 'Cliente detectado', telefono: '600111222' });
    expect(component.form.controls.clienteId.value).toBe(cliente.id);
  });

  it('offers matching existing clients for an explicit choice instead of selecting one automatically', () => {
    presupuestoIaService.generarBorrador.mockReturnValue(of(iaDraft({ clienteNombre: 'Cliente de' })));
    component.textoObraIa = 'Trabajo para el cliente';

    component.generarConIa();

    expect(component.clientesCoincidentes.map((value) => value.id)).toEqual([cliente.id]);
    expect(component.form.controls.clienteId.value).toBeNull();
    component.usarClienteSugerido(component.clientes[0]);
    expect(component.form.controls.clienteId.value).toBe(cliente.id);
  });

  it('offers multiple detected client matches without selecting one automatically', () => {
    component.clientes = [
      { id: 7, nombre: 'Cliente de prueba', telefono: '', email: '', direccion: '', dni: '', fechaCreacion: '' },
      { id: 8, nombre: 'Cliente de reforma', telefono: '', email: '', direccion: '', dni: '', fechaCreacion: '' },
    ];
    presupuestoIaService.generarBorrador.mockReturnValue(of(iaDraft({ clienteNombre: 'Cliente de' })));
    component.textoObraIa = 'Trabajo para el cliente';
    component.generarConIa();

    expect(component.clientesCoincidentes).toHaveLength(2);
    expect(component.form.controls.clienteId.value).toBeNull();
  });

  it('does not assign a client when the draft contains no client suggestion', () => {
    presupuestoIaService.generarBorrador.mockReturnValue(of(iaDraft({ clienteNombre: null, clienteTelefono: null })));
    component.generarConIa();

    expect(component.clienteSugerido).toBeNull();
    expect(component.clientesCoincidentes).toEqual([]);
    expect(component.form.controls.clienteId.value).toBeNull();
  });
});

function iaDraft(overrides: Partial<PresupuestoIaBorradorResponse> = {}): PresupuestoIaBorradorResponse {
  return {
    clienteId: null,
    clienteNombre: null,
    clienteTelefono: null,
    transcripcion: 'Alicatar baño y pintar pared',
    items: [
      {
        materialId: null, materialNombre: null, tareaManual: 'Alicatar baño', cantidad: null,
        precioUnitario: 0, unidad: 'm2', aplicaIva: true, descuentoPorcentaje: 0, descuentoFijo: 0,
        visiblePdf: true, confianza: 'baja', faltaPrecio: true, cantidadDudosa: true,
      },
      {
        materialId: 3, materialNombre: 'Pintura blanca', tareaManual: 'Pintar pared', cantidad: 2,
        precioUnitario: 10, unidad: 'l', aplicaIva: true, descuentoPorcentaje: 0, descuentoFijo: 0,
        visiblePdf: true, confianza: 'media', faltaPrecio: false, cantidadDudosa: false,
      },
    ],
    notaAdicional: null,
    ...overrides,
  };
}
