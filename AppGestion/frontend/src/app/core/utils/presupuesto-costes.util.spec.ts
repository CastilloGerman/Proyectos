import { describe, it, expect } from 'vitest';
import { calcularPresupuestoCostes, PRESUPUESTO_IVA_RATE } from './presupuesto-costes.util';

describe('calcularPresupuestoCostes', () => {
  it('calculates subtotal, IVA and total for a single line without discounts', () => {
    const r = calcularPresupuestoCostes({
      items: [{ cantidad: 2, precioUnitario: 100, aplicaIva: true }],
      ivaHabilitado: true,
    });
    expect(r.subtotalItems).toBe(200);
    expect(r.baseIva).toBe(200);
    expect(r.iva).toBeCloseTo(200 * PRESUPUESTO_IVA_RATE);
    expect(r.total).toBeCloseTo(200 * (1 + PRESUPUESTO_IVA_RATE));
  });

  it('applies line-level percentage and fixed discounts', () => {
    const r = calcularPresupuestoCostes({
      items: [
        {
          cantidad: 10,
          precioUnitario: 50,
          descuentoPorcentaje: 10,
          descuentoFijo: 25,
          aplicaIva: true,
        },
      ],
      ivaHabilitado: true,
    });
    // 10*50=500 → -10%=450 → -25=425
    expect(r.subtotalItems).toBe(425);
    expect(r.iva).toBeCloseTo(425 * PRESUPUESTO_IVA_RATE);
  });

  it('applies global discount before IVA when descuentoAntesIva is true', () => {
    const r = calcularPresupuestoCostes({
      items: [{ cantidad: 1, precioUnitario: 1000, aplicaIva: true }],
      descuentoGlobalPorcentaje: 10,
      descuentoGlobalFijo: 50,
      descuentoAntesIva: true,
      ivaHabilitado: true,
    });
    // subtotal items 1000 → global -10% = 900 → -50 = 850
    expect(r.subtotalItems).toBe(1000);
    expect(r.descuentoTotal).toBe(150);
    expect(r.baseIva).toBe(850);
    expect(r.iva).toBeCloseTo(850 * PRESUPUESTO_IVA_RATE);
    expect(r.total).toBeCloseTo(850 * (1 + PRESUPUESTO_IVA_RATE));
  });

  it('does not reduce baseIva with global discount when descuentoAntesIva is false', () => {
    const r = calcularPresupuestoCostes({
      items: [{ cantidad: 1, precioUnitario: 1000, aplicaIva: true }],
      descuentoGlobalPorcentaje: 10,
      descuentoGlobalFijo: 0,
      descuentoAntesIva: false,
      ivaHabilitado: true,
    });
    expect(r.baseIva).toBe(1000);
    expect(r.iva).toBeCloseTo(1000 * PRESUPUESTO_IVA_RATE);
    // subtotal after discount = 900, total = 900 + iva on full base
    expect(r.total).toBeCloseTo(900 + 1000 * PRESUPUESTO_IVA_RATE);
  });

  it('excludes IVA for lines with aplicaIva false', () => {
    const r = calcularPresupuestoCostes({
      items: [
        { cantidad: 1, precioUnitario: 100, aplicaIva: true },
        { cantidad: 1, precioUnitario: 200, aplicaIva: false },
      ],
      ivaHabilitado: true,
    });
    expect(r.subtotalItems).toBe(300);
    expect(r.baseIva).toBe(100);
    expect(r.iva).toBeCloseTo(100 * PRESUPUESTO_IVA_RATE);
    expect(r.total).toBeCloseTo(300 + 100 * PRESUPUESTO_IVA_RATE);
  });

  it('returns zero IVA when ivaHabilitado is false', () => {
    const r = calcularPresupuestoCostes({
      items: [{ cantidad: 3, precioUnitario: 40, aplicaIva: true }],
      ivaHabilitado: false,
    });
    expect(r.iva).toBe(0);
    expect(r.total).toBe(120);
  });

  it('clamps negative line subtotals to zero', () => {
    const r = calcularPresupuestoCostes({
      items: [{ cantidad: 1, precioUnitario: 10, descuentoFijo: 50, aplicaIva: true }],
      ivaHabilitado: true,
    });
    expect(r.subtotalItems).toBe(0);
    expect(r.total).toBe(0);
  });

  it('sums multiple lines correctly', () => {
    const r = calcularPresupuestoCostes({
      items: [
        { cantidad: 2, precioUnitario: 75, aplicaIva: true },
        { cantidad: 1, precioUnitario: 50, descuentoPorcentaje: 20, aplicaIva: true },
      ],
      ivaHabilitado: true,
    });
    expect(r.subtotalItems).toBe(190); // 150 + 40
    expect(r.iva).toBeCloseTo(190 * PRESUPUESTO_IVA_RATE);
  });
});
