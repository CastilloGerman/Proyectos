export type FiscalCriterio = 'DEVENGO' | 'CAJA';

/** Urgencia del plazo de presentación (Modelo 303), calculada en la API. */
export type FiscalPlazoEstado = 'VERDE' | 'AMARILLO' | 'ROJO';

export interface FiscalPlazoActual {
  trimestre: string;
  fechaLimite: string;
  diasRestantes: number;
  estado: FiscalPlazoEstado;
  mensaje: string;
  plazoAnteriorVencido: boolean;
  mensajeAdvertencia: string | null;
}

export interface Modelo303Resumen {
  anio: number;
  trimestre: number;
  fechaDesde: string;
  fechaHasta: string;
  criterio: FiscalCriterio;
  soloFacturasPagadas: boolean;
  baseImponibleTotal: number;
  ivaRepercutido: number;
  ivaSoportado: number;
  ivaSoportadoCalculado: boolean;
  ivaSoportadoNota: string;
  resultadoIva: number;
  resultadoEsIngreso: boolean;
  numeroFacturas: number;
  advertencias: string[];
  avisoLegal: string;
}

export interface Modelo347Cliente {
  clienteId: number;
  nombre: string;
  dni: string;
  baseImponibleAnual: number;
}

export interface Modelo347Resumen {
  anio: number;
  clientes: Modelo347Cliente[];
  totalClientesUmbral: number;
  umbralEuros: string;
  avisoLegal: string;
}
