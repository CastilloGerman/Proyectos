import { describe, expect, it, vi } from 'vitest';
import { CalculadoraM2Component, CalculadoraResult } from './calculadora-m2.component';

function createComponent(close = vi.fn()) {
  const translate = {
    instant: (key: string, params?: Record<string, unknown>) => {
      if (key === 'calcM2.totalZones') return `${params?.['count']} zonas`;
      if (key === 'calcM2.zoneLine') return `${params?.['name']}: ${params?.['area']} (${params?.['formula']})`;
      if (key === 'calcM2.totalLine') return `Total: ${params?.['area']}`;
      return key;
    },
  };
  return new CalculadoraM2Component(
    { close } as never,
    {},
    translate as never,
  );
}

describe('CalculadoraM2Component', () => {
  it('includes the current calculated zone when inserting with history', () => {
    const close = vi.fn();
    const component = createComponent(close);

    component.history = [
      { area: 10, descripcion: 'Zona guardada', formula: '2 x 5' },
    ];
    component.result = 5;
    component.descripcion = 'Zona actual';
    component.formulaDisplay = '1 x 5';

    expect(component.insertValue).toBe(15);

    component.insert();

    const payload = close.mock.calls[0][0] as CalculadoraResult;
    expect(payload.area).toBe(15);
    expect(payload.descripcion).toBe('2 zonas');
    expect(payload.zonas).toEqual([
      { area: 10, descripcion: 'Zona guardada', formula: '2 x 5' },
      { area: 5, descripcion: 'Zona actual', formula: '1 x 5' },
    ]);
  });
});
