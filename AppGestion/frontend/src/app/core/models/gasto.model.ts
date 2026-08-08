export type GastoCategoria = 'SUMINISTROS' | 'MATERIAL' | 'DIETAS' | 'OTROS';

export interface Gasto {
  id: number;
  proveedor: string;
  concepto: string;
  fecha: string;
  baseImponible: number;
  tipoIva: number;
  cuotaIva: number;
  categoria: GastoCategoria;
}

export interface GastoRequest {
  proveedor: string;
  concepto: string;
  fecha: string;
  baseImponible: number;
  tipoIva: number;
  categoria: GastoCategoria;
}

export const GASTO_CATEGORIAS: GastoCategoria[] = ['SUMINISTROS', 'MATERIAL', 'DIETAS', 'OTROS'];

export const TIPOS_IVA: number[] = [21, 10, 4, 0];

export function calcularCuotaIva(baseImponible: number, tipoIva: number): number {
  return Math.round(baseImponible * tipoIva) / 100;
}
