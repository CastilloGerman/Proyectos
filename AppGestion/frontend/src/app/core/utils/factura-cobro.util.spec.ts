import { describe, it, expect } from 'vitest';
import {
  calcularImportePendiente,
  esImporteParcialValido,
  facturaTieneImportePendiente,
} from './factura-cobro.util';

describe('calcularImportePendiente', () => {
  it('returns 0 for paid invoices', () => {
    expect(calcularImportePendiente({ estadoPago: 'Pagada', total: 1000 })).toBe(0);
  });

  it('returns full total for unpaid invoices', () => {
    expect(calcularImportePendiente({ estadoPago: 'No Pagada', total: 500 })).toBe(500);
  });

  it('returns difference for partial payments', () => {
    expect(
      calcularImportePendiente({ estadoPago: 'Parcial', total: 1000, montoCobrado: 400 }),
    ).toBe(600);
  });

  it('returns 0 when partial payment covers total within epsilon', () => {
    expect(
      calcularImportePendiente({ estadoPago: 'Parcial', total: 100, montoCobrado: 99.995 }),
    ).toBeCloseTo(0.005, 3);
  });
});

describe('facturaTieneImportePendiente', () => {
  it('returns false when fully paid', () => {
    expect(facturaTieneImportePendiente({ estadoPago: 'Pagada', total: 200 })).toBe(false);
  });

  it('returns true for unpaid invoice', () => {
    expect(facturaTieneImportePendiente({ estadoPago: 'No Pagada', total: 200 })).toBe(true);
  });

  it('returns false when partial cobro leaves negligible pending amount', () => {
    expect(
      facturaTieneImportePendiente({ estadoPago: 'Parcial', total: 100, montoCobrado: 99.995 }),
    ).toBe(false);
  });

  it('returns true when partial cobro leaves meaningful pending amount', () => {
    expect(
      facturaTieneImportePendiente({ estadoPago: 'Parcial', total: 100, montoCobrado: 50 }),
    ).toBe(true);
  });
});

describe('esImporteParcialValido', () => {
  it('accepts import strictly between 0 and total', () => {
    expect(esImporteParcialValido(50, 100)).toBe(true);
  });

  it('rejects zero or negative import', () => {
    expect(esImporteParcialValido(0, 100)).toBe(false);
    expect(esImporteParcialValido(-10, 100)).toBe(false);
  });

  it('rejects import above total', () => {
    expect(esImporteParcialValido(100.01, 100)).toBe(false);
  });

  it('rejects import equal to total within epsilon', () => {
    expect(esImporteParcialValido(99.996, 100)).toBe(false);
  });
});
