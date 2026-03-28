export type FiscalCriterio = 'DEVENGO' | 'CAJA';

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
