import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslateModule } from '@ngx-translate/core';
import { Observable, of, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../core/auth/auth.service';
import { Factura } from '../../core/models/factura.model';
import { Gasto } from '../../core/models/gasto.model';
import { Presupuesto } from '../../core/models/presupuesto.model';
import { FacturaService } from '../../core/services/factura.service';
import { GastoService } from '../../core/services/gasto.service';
import { MaterialService } from '../../core/services/material.service';
import { PresupuestoService } from '../../core/services/presupuesto.service';
import { FacturaParcialImporteDialogComponent } from '../../shared/factura-parcial-importe-dialog/factura-parcial-importe-dialog.component';
import { DashboardComponent } from './dashboard.component';

/** 15 de mayo de 2026, 12:00 local: segundo trimestre, mes anterior = abril. */
const AHORA = new Date(2026, 4, 15, 12, 0, 0);

function presupuesto(partial: Partial<Presupuesto> = {}): Presupuesto {
  return {
    id: 1,
    clienteId: 1,
    clienteNombre: 'Cliente',
    fechaCreacion: '2026-02-01T12:00:00',
    subtotal: 0,
    iva: 0,
    total: 0,
    ivaHabilitado: true,
    estado: 'Pendiente',
    items: [],
    ...partial,
  };
}

function factura(partial: Partial<Factura> = {}): Factura {
  return {
    id: 1,
    numeroFactura: 'F-1',
    clienteId: 1,
    clienteNombre: 'Cliente',
    fechaCreacion: '2026-02-01T12:00:00',
    subtotal: 0,
    iva: 0,
    total: 0,
    ivaHabilitado: true,
    metodoPago: 'Transferencia',
    estadoPago: 'No Pagada',
    items: [],
    ...partial,
  };
}

function gasto(partial: Partial<Gasto> = {}): Gasto {
  return {
    id: 1,
    proveedor: 'Proveedor',
    concepto: 'Material',
    fecha: '2026-05-10',
    baseImponible: 0,
    tipoIva: 21,
    cuotaIva: 0,
    categoria: 'MATERIAL',
    ...partial,
  };
}

interface Carga {
  presupuestos?: Presupuesto[];
  facturas?: Factura[];
  gastos?: Gasto[];
  presupuestos$?: Observable<Presupuesto[]>;
  facturas$?: Observable<Factura[]>;
  gastos$?: Observable<Gasto[]>;
  materiales$?: Observable<never[]>;
  canMutate?: boolean;
  afterClosed?: Observable<number | undefined>;
  actualizarEstadoPago?: ReturnType<typeof vi.fn>;
}

async function montar(carga: Carga = {}): Promise<{
  fixture: ComponentFixture<DashboardComponent>;
  snackBar: { open: ReturnType<typeof vi.fn> };
  dialog: { open: ReturnType<typeof vi.fn> };
  actualizarEstadoPago: ReturnType<typeof vi.fn>;
}> {
  const snackBar = { open: vi.fn() };
  const actualizarEstadoPago = carga.actualizarEstadoPago ?? vi.fn().mockReturnValue(of(factura()));
  const dialog = {
    open: vi.fn().mockReturnValue({ afterClosed: () => carga.afterClosed ?? of(undefined) }),
  };

  await TestBed.configureTestingModule({
    imports: [DashboardComponent, NoopAnimationsModule, TranslateModule.forRoot()],
    providers: [
      provideRouter([]),
      { provide: AuthService, useValue: { canMutate: () => carga.canMutate ?? false } },
      {
        provide: PresupuestoService,
        useValue: {
          getAll: () => carga.presupuestos$ ?? of(carga.presupuestos ?? []),
          actualizarEstado: vi.fn().mockReturnValue(of(presupuesto())),
        },
      },
      {
        provide: FacturaService,
        useValue: {
          getAll: () => carga.facturas$ ?? of(carga.facturas ?? []),
          actualizarEstadoPago,
        },
      },
      {
        provide: MaterialService,
        useValue: { getTopUsados: () => carga.materiales$ ?? of([]) },
      },
      {
        provide: GastoService,
        useValue: { getAll: () => carga.gastos$ ?? of(carga.gastos ?? []) },
      },
      { provide: MatDialog, useValue: dialog },
      { provide: MatSnackBar, useValue: snackBar },
    ],
  });
  // MatDialogModule y MatSnackBarModule van en los imports del componente y
  // ganan al provider del TestBed. El override queda solo en este spec.
  TestBed.overrideComponent(DashboardComponent, {
    add: {
      providers: [
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snackBar },
      ],
    },
  });
  await TestBed.compileComponents();

  const fixture = TestBed.createComponent(DashboardComponent);
  fixture.detectChanges();
  return { fixture, snackBar, dialog, actualizarEstadoPago };
}

describe('DashboardComponent', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(AHORA);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('cuenta estados con y sin tilde, el desconocido como pendiente y el total ausente como 0', async () => {
    const { fixture } = await montar({
      presupuestos: [
        presupuesto({ id: 1, estado: 'Pendiente', total: 100 }),
        presupuesto({ id: 2, estado: 'Aceptado', total: 40 }),
        presupuesto({ id: 3, estado: 'En ejecución', total: 10 }),
        presupuesto({ id: 4, estado: 'En ejecucion', total: 15 }),
        presupuesto({ id: 5, estado: 'Rechazado', total: 5 }),
        presupuesto({ id: 6, estado: 'Archivado', total: 7 }),
        presupuesto({
          id: 7,
          estado: 'Pendiente',
          total: undefined as unknown as number,
          fechaCreacion: '2020-01-01T12:00:00',
        }),
      ],
    });

    expect(fixture.componentInstance.presupuestoStats).toEqual({
      pendientes: 3,
      aceptados: 1,
      enEjecucion: 2,
      rechazados: 1,
      totalValor: 177,
    });
    expect(fixture.nativeElement.textContent).toContain('dashboard.legendInProgress 2');
  });

  it('suma el total de la pagada, el monto cobrado del parcial y la mitad si el parcial no trae monto', async () => {
    const { fixture } = await montar({
      facturas: [
        factura({ id: 1, estadoPago: 'Pagada', total: 100, montoCobrado: 1 }),
        factura({ id: 2, estadoPago: 'Parcial', total: 80, montoCobrado: 30 }),
        factura({ id: 3, estadoPago: 'Parcial', total: 40 }),
        factura({ id: 4, estadoPago: 'No Pagada', total: 10 }),
      ],
    });

    expect(fixture.componentInstance.facturaStats).toEqual({
      pagadas: 1,
      noPagadas: 1,
      parciales: 2,
      totalFacturado: 230,
      totalCobrado: 150,
      totalPendiente: 80,
    });
  });

  it('separa vencidas de las que vencen en 7 días y no cuenta las que no tienen fecha', async () => {
    const { fixture } = await montar({
      facturas: [
        factura({ id: 1, estadoPago: 'No Pagada', total: 50, fechaVencimiento: '2026-05-14T12:00:00' }),
        factura({ id: 2, estadoPago: 'No Pagada', total: 11, fechaVencimiento: '2026-05-15T12:00:00' }),
        factura({ id: 3, estadoPago: 'No Pagada', total: 7, fechaVencimiento: '2026-05-22T12:00:00' }),
        factura({ id: 4, estadoPago: 'No Pagada', total: 9, fechaVencimiento: '2026-05-23T12:00:00' }),
        factura({ id: 5, estadoPago: 'No Pagada', total: 80 }),
        factura({ id: 6, estadoPago: 'Pagada', total: 100, fechaVencimiento: '2026-05-01T12:00:00' }),
      ],
    });

    expect(fixture.componentInstance.saludCobros).toMatchObject({
      vencidas: 1,
      importeVencido: 50,
      proximasAVencer: 2,
      importeProximas: 18,
    });
  });

  it('deja el ratio de cobro en 0 cuando no hay importe facturado', async () => {
    const { fixture } = await montar({
      facturas: [factura({ id: 1, estadoPago: 'No Pagada', total: 0 })],
    });

    expect(fixture.componentInstance.facturaStats.totalFacturado).toBe(0);
    expect(fixture.componentInstance.saludCobros.ratioCobro).toBe(0);
  });

  it('topa el ratio de cobro en 100 cuando lo cobrado supera lo facturado', async () => {
    const { fixture } = await montar({
      facturas: [factura({ id: 1, estadoPago: 'Parcial', total: 100, montoCobrado: 250 })],
    });

    expect(fixture.componentInstance.facturaStats.totalCobrado).toBe(250);
    expect(fixture.componentInstance.facturaStats.totalPendiente).toBe(-150);
    expect(fixture.componentInstance.saludCobros.ratioCobro).toBe(100);
  });

  it('suma los gastos del trimestre natural, acepta fecha con hora y deja fuera el resto', async () => {
    const { fixture } = await montar({
      gastos: [
        gasto({ id: 1, fecha: '2026-05-10', baseImponible: 100, cuotaIva: 21 }),
        gasto({ id: 2, fecha: '2026-04-02T23:30:00', baseImponible: 40, cuotaIva: 4 }),
        gasto({ id: 3, fecha: '2026-06-30', baseImponible: 10, cuotaIva: 2 }),
        gasto({ id: 4, fecha: '2026-03-31', baseImponible: 999, cuotaIva: 99 }),
        gasto({ id: 5, fecha: '2026-07-01T08:00:00', baseImponible: 999, cuotaIva: 99 }),
      ],
    });

    expect(fixture.componentInstance.gastoTrimestreStats).toEqual({
      year: 2026,
      trimestre: 2,
      count: 3,
      totalBase: 150,
      totalIva: 27,
      totalConIva: 177,
    });
  });

  it('suma presupuestos y facturas del mismo cliente, usa Sin nombre y se queda con 5', async () => {
    let id = 1;
    const presupuestos = [
      ...Array.from({ length: 4 }, () => presupuesto({ id: id++, clienteId: 1, clienteNombre: 'Ana' })),
      ...Array.from({ length: 3 }, () => presupuesto({ id: id++, clienteId: 2, clienteNombre: 'Bea' })),
      ...Array.from({ length: 3 }, () => presupuesto({ id: id++, clienteId: 4, clienteNombre: 'Dani' })),
      presupuesto({ id: id++, clienteId: 6, clienteNombre: 'Fran' }),
    ];
    const facturas = [
      ...Array.from({ length: 2 }, () => factura({ id: id++, clienteId: 1, clienteNombre: 'Ana' })),
      ...Array.from({ length: 2 }, () => factura({ id: id++, clienteId: 2, clienteNombre: 'Bea' })),
      ...Array.from({ length: 4 }, () =>
        factura({ id: id++, clienteId: 3, clienteNombre: undefined as unknown as string }),
      ),
      ...Array.from({ length: 2 }, () => factura({ id: id++, clienteId: 5, clienteNombre: 'Eva' })),
    ];

    const { fixture } = await montar({ presupuestos, facturas });

    expect(fixture.componentInstance.topClientes).toEqual([
      { clienteId: 1, clienteNombre: 'Ana', count: 6 },
      { clienteId: 2, clienteNombre: 'Bea', count: 5 },
      { clienteId: 3, clienteNombre: 'Sin nombre', count: 4 },
      { clienteId: 4, clienteNombre: 'Dani', count: 3 },
      { clienteId: 5, clienteNombre: 'Eva', count: 2 },
    ]);
  });

  describe('gráfico de ingresos', () => {
    const facturasGrafico = [
      factura({ id: 1, total: 10, fechaCreacion: '2026-01-10T12:00:00' }),
      factura({ id: 2, total: 5, fechaCreacion: '2026-01-20T12:00:00' }),
      factura({ id: 3, total: 20, fechaCreacion: '2026-04-02T12:00:00' }),
      factura({ id: 4, total: 7, fechaCreacion: '2025-06-15T12:00:00' }),
    ];

    it('en modo año devuelve 12 puntos, ceros incluidos, y el último mes es el mes natural anterior', async () => {
      const { fixture } = await montar({ facturas: facturasGrafico });
      const dash = fixture.componentInstance;

      expect(dash.chartIngresosMode).toBe('anio');
      expect(dash.ingresosPorMes.map((punto) => punto.value)).toEqual([15, 0, 0, 20, 0, 0, 0, 0, 0, 0, 0, 0]);
      expect(dash.ingresosPorMes.map((punto) => punto.key)).toEqual([
        '2026-01',
        '2026-02',
        '2026-03',
        '2026-04',
        '2026-05',
        '2026-06',
        '2026-07',
        '2026-08',
        '2026-09',
        '2026-10',
        '2026-11',
        '2026-12',
      ]);

      dash.onChartIngresosModeChange('ultimoMes');

      expect(dash.ingresosPorMes).toEqual([
        { name: 'dashboard.month.apr 2026', value: 20, key: '2026-04' },
      ]);
      expect(dash.monthDrill).toBeNull();
    });

    it('el click rellena importe y conteo, y cambiar de año limpia el detalle', async () => {
      const { fixture } = await montar({ facturas: facturasGrafico });
      const dash = fixture.componentInstance;

      dash.onChartMonthClick(new MouseEvent('click'), 0);

      expect(dash.monthDrill).toEqual({
        label: 'dashboard.month.jan 2026',
        value: 15,
        facturasCount: 2,
        key: '2026-01',
      });

      dash.onChartYearChange(2025);

      expect(dash.monthDrill).toBeNull();
      expect(dash.hoveredChartMonth).toBeNull();
      expect(dash.selectedChartYear).toBe(2025);
      expect(dash.ingresosPorMes.map((punto) => punto.value)).toEqual([0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0]);
    });
  });

  describe('carga y cambio de estado', () => {
    it.each([
      {
        lista: 'presupuestos',
        snack: 'snack.estimatesLoadFail',
        carga: { presupuestos$: throwError(() => new Error('red')) },
        vacia: (dash: DashboardComponent) => dash.recentPresupuestos,
      },
      {
        lista: 'facturas',
        snack: 'snack.invoiceLoadFail',
        carga: { facturas$: throwError(() => new Error('red')) },
        vacia: (dash: DashboardComponent) => dash.recentFacturas,
      },
      {
        lista: 'gastos',
        snack: 'gastos.loadFail',
        carga: { gastos$: throwError(() => new Error('red')) },
        vacia: (dash: DashboardComponent) => dash.recentGastos,
      },
    ])('si falla getAll de $lista abre el snack y esa lista queda vacía', async ({ snack, carga, vacia }) => {
      const { fixture, snackBar } = await montar({
        presupuestos: [presupuesto({ id: 1 })],
        facturas: [factura({ id: 1 })],
        gastos: [gasto({ id: 1 })],
        ...carga,
      });

      expect(snackBar.open).toHaveBeenCalledWith(snack, 'common.close', { duration: 5000 });
      expect(vacia(fixture.componentInstance)).toEqual([]);
    });

    it('no actualiza el cobro si el diálogo parcial se cierra sin importe', async () => {
      const pendiente = factura({ id: 9, estadoPago: 'No Pagada', total: 100 });
      const { fixture, dialog, actualizarEstadoPago } = await montar({
        facturas: [pendiente],
        afterClosed: of(undefined),
      });

      fixture.componentInstance.cambiarEstadoFactura(pendiente, 'Parcial');

      expect(dialog.open).toHaveBeenCalledWith(
        FacturaParcialImporteDialogComponent,
        expect.objectContaining({
          data: expect.objectContaining({ totalFactura: 100, numeroFactura: 'F-1' }),
        }),
      );
      expect(actualizarEstadoPago).not.toHaveBeenCalled();
      expect(fixture.componentInstance.recentFacturas[0].estadoPago).toBe('No Pagada');
    });

    it('con importe actualiza el cobro, el badge y las barras, y recalcula el punto del mes con el mismo total', async () => {
      const pendiente = factura({
        id: 9,
        numeroFactura: 'F-9',
        clienteNombre: 'Norte',
        estadoPago: 'No Pagada',
        total: 100,
        fechaCreacion: '2026-03-15T12:00:00',
      });
      const actualizarEstadoPago = vi.fn().mockImplementation((_id: number, estadoPago: string, montoCobrado?: number) =>
        of({ ...pendiente, estadoPago, montoCobrado }),
      );
      const { fixture } = await montar({
        facturas: [pendiente],
        afterClosed: of(40),
        actualizarEstadoPago,
      });
      const serieAntes = fixture.componentInstance.ingresosPorMes;

      fixture.componentInstance.cambiarEstadoFactura(pendiente, 'Parcial');
      fixture.detectChanges();

      expect(actualizarEstadoPago).toHaveBeenCalledWith(9, 'Parcial', 40);
      expect(fixture.componentInstance.recentFacturas[0].estadoPago).toBe('Parcial');
      expect(fixture.componentInstance.recentFacturas[0].montoCobrado).toBe(40);
      expect(fixture.componentInstance.facturaStats).toMatchObject({
        parciales: 1,
        noPagadas: 0,
        totalCobrado: 40,
        totalPendiente: 60,
      });
      expect(fixture.nativeElement.querySelector('.badge-parcial')).not.toBeNull();
      expect(fixture.nativeElement.textContent).toContain('dashboard.legendPartial 1');
      expect(fixture.componentInstance.ingresosPorMes).not.toBe(serieAntes);
      expect(fixture.componentInstance.ingresosPorMes[2]).toMatchObject({ key: '2026-03', value: 100 });
    });
  });

  it('oculta el menú del badge cuando el usuario no puede modificar', async () => {
    const { fixture } = await montar({
      canMutate: false,
      presupuestos: [presupuesto({ id: 1, estado: 'Pendiente', clienteNombre: 'Ana' })],
      facturas: [factura({ id: 1, estadoPago: 'No Pagada', numeroFactura: 'F-1', clienteNombre: 'Ana' })],
    });

    expect(fixture.nativeElement.querySelectorAll('.badge-menu-trigger')).toHaveLength(0);
    expect(fixture.nativeElement.querySelectorAll('.estado-badge')).toHaveLength(2);
  });
});
