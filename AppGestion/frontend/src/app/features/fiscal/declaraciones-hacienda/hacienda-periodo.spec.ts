import { describe, expect, it } from 'vitest';
import {
  diaEnMadrid,
  ocultarPlazo,
  plazoEstaOculto,
  trimestrePorDefecto,
} from './hacienda-periodo';

describe('trimestrePorDefecto', () => {
  it('el 20 de octubre el plazo del tercer trimestre sigue abierto', () => {
    expect(trimestrePorDefecto(diaEnMadrid(instanteMadrid(2026, 10, 20)))).toEqual({
      year: 2026,
      trimestre: 3,
    });
  });

  it('cuenta el 20 de octubre aunque en UTC siga siendo el 19', () => {
    expect(trimestrePorDefecto(diaEnMadrid(new Date('2026-10-19T22:30:00Z')))).toEqual({
      year: 2026,
      trimestre: 3,
    });
  });

  it('el 21 de octubre abre el trimestre en curso', () => {
    expect(trimestrePorDefecto(diaEnMadrid(instanteMadrid(2026, 10, 21)))).toEqual({
      year: 2026,
      trimestre: 4,
    });
  });

  it('el 5 de enero abre el cuarto trimestre del año anterior', () => {
    expect(trimestrePorDefecto(diaEnMadrid(instanteMadrid(2027, 1, 5)))).toEqual({
      year: 2026,
      trimestre: 4,
    });
  });

  it('el 31 de enero abre el primer trimestre del año en curso', () => {
    expect(trimestrePorDefecto(diaEnMadrid(instanteMadrid(2027, 1, 31)))).toEqual({
      year: 2027,
      trimestre: 1,
    });
  });

  it('el 1 de marzo abre el primer trimestre del año en curso', () => {
    expect(trimestrePorDefecto(diaEnMadrid(instanteMadrid(2027, 3, 1)))).toEqual({
      year: 2027,
      trimestre: 1,
    });
  });
});

describe('plazo oculto', () => {
  it('recuerda el trimestre y no rompe si el almacenamiento falla', () => {
    const memoria = new Map<string, string>();
    const almacenamiento = {
      getItem: (clave: string) => memoria.get(clave) ?? null,
      setItem: (clave: string, valor: string) => memoria.set(clave, valor),
    };
    const periodo = { year: 2026, trimestre: 3 as const };
    expect(plazoEstaOculto(periodo, almacenamiento)).toBe(false);
    ocultarPlazo(periodo, almacenamiento);
    expect(plazoEstaOculto(periodo, almacenamiento)).toBe(true);
    expect(plazoEstaOculto({ year: 2026, trimestre: 4 }, almacenamiento)).toBe(false);

    const roto = {
      getItem: () => {
        throw new Error('bloqueado');
      },
      setItem: () => {
        throw new Error('bloqueado');
      },
    };
    expect(plazoEstaOculto(periodo, roto)).toBe(false);
    expect(() => ocultarPlazo(periodo, roto)).not.toThrow();
  });
});

/** Un instante cuyo calendario en Europe/Madrid es ese día. */
function instanteMadrid(year: number, month: number, day: number): Date {
  let utc = Date.UTC(year, month - 1, day, 0, 0, 0);
  for (let hora = 0; hora < 30; hora++) {
    const instante = new Date(utc);
    const madrid = diaEnMadrid(instante);
    if (madrid.year === year && madrid.month === month && madrid.day === day) {
      return instante;
    }
    utc += 60 * 60 * 1000;
  }
  throw new Error(`sin instante de Madrid para ${year}-${month}-${day}`);
}
