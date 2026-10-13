import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import { Observable, Subject } from 'rxjs';
import {
  ETIQUETA_A_INGRESAR,
  ETIQUETA_SIN_DATOS,
  FiltroModelo303,
  cifraModelo303,
  observacionesModelo303,
} from './hacienda-carga';

describe('cifraModelo303', () => {
  it('sin facturas ni gastos no enseña 0,00 € como ingreso', () => {
    expect(cifraModelo303(0, 0, 0)).toEqual({ etiqueta: ETIQUETA_SIN_DATOS, importe: null });
  });

  it('una factura de 0 € sigue siendo IVA estimado a ingresar 0', () => {
    expect(cifraModelo303(1, 0, 0)).toEqual({ etiqueta: ETIQUETA_A_INGRESAR, importe: 0 });
  });

  it('repercutido igual a soportado sigue siendo IVA estimado a ingresar 0', () => {
    expect(cifraModelo303(4, 2, 21 - 21)).toEqual({ etiqueta: ETIQUETA_A_INGRESAR, importe: 0 });
  });
});

describe('observacionesModelo303', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('cancela la petición anterior y no deja que una respuesta tardía pise la última selección', () => {
    const filtros = new Subject<FiltroModelo303>();
    const recibidos: number[] = [];

    observacionesModelo303(filtros, (filtro) => espera(filtro.trimestre === 1 ? 2000 : 20, filtro.trimestre)).subscribe(
      (trimestre) => recibidos.push(trimestre),
    );

    filtros.next(filtro(1));
    vi.advanceTimersByTime(300);
    filtros.next(filtro(2));
    vi.advanceTimersByTime(300);
    vi.advanceTimersByTime(20);
    vi.advanceTimersByTime(2000);

    expect(recibidos).toEqual([2]);
  });

  it('agrupa cambios seguidos y solo pide la última selección', () => {
    const filtros = new Subject<FiltroModelo303>();
    const pedidos: number[] = [];

    observacionesModelo303(filtros, (filtro) => {
      pedidos.push(filtro.trimestre);
      return espera(0, filtro.trimestre);
    }).subscribe();

    filtros.next(filtro(1));
    vi.advanceTimersByTime(100);
    filtros.next(filtro(4));
    vi.advanceTimersByTime(300);

    expect(pedidos).toEqual([4]);
  });
});

function filtro(trimestre: number): FiltroModelo303 {
  return { year: 2026, trimestre, criterio: 'DEVENGO', soloPagadas: false };
}

function espera<T>(ms: number, valor: T): Observable<T> {
  return new Observable((subscriber) => {
    const handle = setTimeout(() => {
      subscriber.next(valor);
      subscriber.complete();
    }, ms);
    return () => clearTimeout(handle);
  });
}
