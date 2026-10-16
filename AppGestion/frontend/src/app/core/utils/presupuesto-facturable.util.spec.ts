import { describe, expect, it } from 'vitest';
import { presupuestoSePuedeFacturar } from './presupuesto-facturable.util';

describe('presupuestoSePuedeFacturar', () => {
  it('admite pendiente, aceptado y en ejecución si no hay factura', () => {
    expect(presupuestoSePuedeFacturar({ estado: 'Pendiente' })).toBe(true);
    expect(presupuestoSePuedeFacturar({ estado: 'Aceptado', facturaId: null })).toBe(true);
    expect(presupuestoSePuedeFacturar({ estado: 'En ejecución' })).toBe(true);
    expect(presupuestoSePuedeFacturar({ estado: 'en ejecucion' })).toBe(true);
  });

  it('excluye rechazados, ya facturados y estados vacíos', () => {
    expect(presupuestoSePuedeFacturar({ estado: 'Rechazado' })).toBe(false);
    expect(presupuestoSePuedeFacturar({ estado: 'Pendiente', facturaId: 12 })).toBe(false);
    expect(presupuestoSePuedeFacturar({ estado: '  ' })).toBe(false);
    expect(presupuestoSePuedeFacturar({})).toBe(false);
  });
});
