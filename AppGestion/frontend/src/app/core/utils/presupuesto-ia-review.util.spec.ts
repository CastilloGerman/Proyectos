import { describe, expect, it } from 'vitest';
import {
  esCantidadPrecioValido,
  esSugerenciaPresupuestoIaSegura,
  evaluarPendientesPresupuestoIa,
  parsePresupuestoDecimal,
} from './presupuesto-ia-review.util';

describe('presupuesto-ia-review.util', () => {
  it('parses decimal comma values and rejects invalid input', () => {
    expect(parsePresupuestoDecimal('12,5')).toBe(12.5);
    expect(parsePresupuestoDecimal('NaN')).toBeNull();
    expect(esCantidadPrecioValido('0,5', '10.25')).toBe(true);
    expect(esCantidadPrecioValido(0, 10)).toBe(false);
  });

  it('counts unresolved AI price, quantity and review flags', () => {
    expect(evaluarPendientesPresupuestoIa([
      {
        iaSugerida: true, iaRevisada: false, confianza: 'alta', materialId: 3, materialEnCatalogo: true,
        faltaPrecio: true, cantidadDudosa: true, cantidad: null, precioUnitario: 0,
      },
      {
        iaSugerida: true, iaRevisada: true, confianza: 'media', materialId: null, materialEnCatalogo: false,
        faltaPrecio: false, cantidadDudosa: false, cantidad: 2, precioUnitario: 5,
      },
      {
        iaSugerida: false, iaRevisada: false, confianza: 'alta', materialId: 3, materialEnCatalogo: true,
        faltaPrecio: true, cantidadDudosa: true, cantidad: 0, precioUnitario: 0,
      },
    ])).toEqual({ precio: 1, cantidad: 1, revision: 1 });
  });

  it('only considers reviewed-catalog matches with a high-confidence complete price and quantity safe', () => {
    const suggestion = {
      iaSugerida: true,
      iaRevisada: false,
      confianza: 'alta',
      materialId: 3,
      materialEnCatalogo: true,
      faltaPrecio: false,
      cantidadDudosa: false,
      cantidad: 2,
      precioUnitario: 10,
    };
    expect(esSugerenciaPresupuestoIaSegura(suggestion)).toBe(true);
    expect(esSugerenciaPresupuestoIaSegura({ ...suggestion, materialEnCatalogo: false })).toBe(false);
    expect(esSugerenciaPresupuestoIaSegura({ ...suggestion, confianza: 'baja' })).toBe(false);
    expect(esSugerenciaPresupuestoIaSegura({ ...suggestion, iaRevisada: true })).toBe(false);
  });
});
