import { describe, expect, it } from 'vitest';
import { MemoriaTipoIva, tipoAlVolverAMarcarIva } from './factura-iva-cabecera';

describe('tipo al volver a marcar el IVA de la factura', () => {
  it('recupera 21, 10, 4 o 0, el tipo que tenía la línea', () => {
    expect(tipoAlVolverAMarcarIva(21, true)).toBe(21);
    expect(tipoAlVolverAMarcarIva(10, true)).toBe(10);
    expect(tipoAlVolverAMarcarIva(4, true)).toBe(4);
    expect(tipoAlVolverAMarcarIva(0, true)).toBe(0);
  });

  it('una línea añadida con el IVA desmarcado vuelve a 21', () => {
    expect(tipoAlVolverAMarcarIva(undefined, true)).toBe(21);
  });

  it('no pisa un tipo ya cargado si el IVA no se había desmarcado', () => {
    expect(tipoAlVolverAMarcarIva(undefined, false)).toBeNull();
  });
});

describe('MemoriaTipoIva', () => {
  it('al desmarcar y volver a marcar restaura el tipo de cada línea', () => {
    const memoria = new MemoriaTipoIva();
    const general = {};
    const reducido = {};
    const superreducido = {};
    const cero = {};

    expect(memoria.apagar(general, 21)).toBe(0);
    expect(memoria.apagar(reducido, 10)).toBe(0);
    expect(memoria.apagar(superreducido, 4)).toBe(0);
    expect(memoria.apagar(cero, 0)).toBe(0);

    expect(memoria.encender(general, true)).toBe(21);
    expect(memoria.encender(reducido, true)).toBe(10);
    expect(memoria.encender(superreducido, true)).toBe(4);
    expect(memoria.encender(cero, true)).toBe(0);
  });

  it('un segundo apagado no sustituye el tipo recordado por 0', () => {
    const memoria = new MemoriaTipoIva();
    const linea = {};
    memoria.apagar(linea, 10);
    expect(memoria.apagar(linea, 0)).toBe(0);
    expect(memoria.encender(linea, true)).toBe(10);
  });

  it('una línea nueva, sin memoria, queda en 21 si estaba deshabilitada', () => {
    const memoria = new MemoriaTipoIva();
    const nueva = {};
    expect(memoria.encender(nueva, true)).toBe(21);
  });
});
