/** Día de calendario en Europe/Madrid, no en la zona del navegador. */
export interface DiaMadrid {
  year: number;
  month: number;
  day: number;
}

export interface TrimestreId {
  year: number;
  trimestre: 1 | 2 | 3 | 4;
}

const ORDINALES = ['', '1.er', '2.º', '3.er', '4.º'] as const;
const MESES = ['', 'ene-mar', 'abr-jun', 'jul-sep', 'oct-dic'] as const;

/** «4.º trimestre (oct-dic)». */
export function etiquetaTrimestre(trimestre: number): string {
  return `${ORDINALES[trimestre]} trimestre (${MESES[trimestre]})`;
}

/** «3.er trimestre, jul-sep». */
export function etiquetaPlazo(trimestre: number): string {
  return `${ORDINALES[trimestre]} trimestre, ${MESES[trimestre]}`;
}

export function diaEnMadrid(instante: Date): DiaMadrid {
  const partes = new Intl.DateTimeFormat('en-US', {
    timeZone: 'Europe/Madrid',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(instante);
  const valor = (tipo: Intl.DateTimeFormatPartTypes) =>
    Number(partes.find((parte) => parte.type === tipo)?.value);
  return { year: valor('year'), month: valor('month'), day: valor('day') };
}

/**
 * Último trimestre ya cerrado cuyo plazo de presentación (20 abr / 20 jul / 20 oct / 30 ene)
 * sigue abierto, incluido el día del límite. Si no hay ninguno, el trimestre en curso.
 */
export function trimestrePorDefecto(hoy: DiaMadrid): TrimestreId {
  const candidatos: TrimestreId[] = [];
  for (const year of [hoy.year - 1, hoy.year]) {
    for (const trimestre of [1, 2, 3, 4] as const) {
      const periodo: TrimestreId = { year, trimestre };
      if (periodoCerrado(periodo, hoy) && plazoAbierto(periodo, hoy)) {
        candidatos.push(periodo);
      }
    }
  }
  if (candidatos.length === 0) {
    return trimestreEnCurso(hoy);
  }
  return candidatos[candidatos.length - 1];
}

export function parseEtiquetaPlazo(etiqueta: string): TrimestreId | null {
  const coincidencia = /^T([1-4])\s+(\d{4})$/.exec(etiqueta.trim());
  if (!coincidencia) {
    return null;
  }
  return { trimestre: Number(coincidencia[1]) as TrimestreId['trimestre'], year: Number(coincidencia[2]) };
}

/** Ámbar salvo que queden 2 días o menos (el día del límite cuenta como 0). */
export function plazoEnRojo(diasRestantes: number): boolean {
  return diasRestantes <= 2;
}

const CLAVE_OCULTO = 'hacienda.plazo.oculto';

export function clavePlazoOculto(periodo: TrimestreId): string {
  return `${CLAVE_OCULTO}.${periodo.year}.T${periodo.trimestre}`;
}

export function plazoEstaOculto(
  periodo: TrimestreId,
  almacenamiento: { getItem(clave: string): string | null },
): boolean {
  try {
    return almacenamiento.getItem(clavePlazoOculto(periodo)) === '1';
  } catch {
    return false;
  }
}

export function ocultarPlazo(
  periodo: TrimestreId,
  almacenamiento: { setItem(clave: string, valor: string): void },
): void {
  try {
    almacenamiento.setItem(clavePlazoOculto(periodo), '1');
  } catch {
    // El navegador puede bloquear el almacenamiento. El aviso se oculta igual en esta visita.
  }
}

function fechaLimite(periodo: TrimestreId): DiaMadrid {
  if (periodo.trimestre === 1) {
    return { year: periodo.year, month: 4, day: 20 };
  }
  if (periodo.trimestre === 2) {
    return { year: periodo.year, month: 7, day: 20 };
  }
  if (periodo.trimestre === 3) {
    return { year: periodo.year, month: 10, day: 20 };
  }
  return { year: periodo.year + 1, month: 1, day: 30 };
}

function finDePeriodo(periodo: TrimestreId): DiaMadrid {
  const fines: Record<TrimestreId['trimestre'], DiaMadrid> = {
    1: { year: periodo.year, month: 3, day: 31 },
    2: { year: periodo.year, month: 6, day: 30 },
    3: { year: periodo.year, month: 9, day: 30 },
    4: { year: periodo.year, month: 12, day: 31 },
  };
  return fines[periodo.trimestre];
}

function periodoCerrado(periodo: TrimestreId, hoy: DiaMadrid): boolean {
  return comparar(hoy, finDePeriodo(periodo)) > 0;
}

function plazoAbierto(periodo: TrimestreId, hoy: DiaMadrid): boolean {
  return comparar(hoy, fechaLimite(periodo)) <= 0;
}

function trimestreEnCurso(hoy: DiaMadrid): TrimestreId {
  const trimestre = hoy.month <= 3 ? 1 : hoy.month <= 6 ? 2 : hoy.month <= 9 ? 3 : 4;
  return { year: hoy.year, trimestre };
}

function comparar(a: DiaMadrid, b: DiaMadrid): number {
  if (a.year !== b.year) {
    return a.year - b.year;
  }
  if (a.month !== b.month) {
    return a.month - b.month;
  }
  return a.day - b.day;
}
