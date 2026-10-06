export type PresupuestoIaConfianza = 'alta' | 'media' | 'baja';

/** Contrato de respuesta de PresupuestoIaBorradorResponse en backend. */
export interface PresupuestoIaBorradorResponse {
  clienteId: number | null;
  clienteNombre: string | null;
  clienteTelefono: string | null;
  transcripcion: string;
  items: PresupuestoIaItemBorradorResponse[];
  notaAdicional: string | null;
}

/** Contrato de PresupuestoIaItemBorradorResponse en backend. */
export interface PresupuestoIaItemBorradorResponse {
  materialId: number | null;
  materialNombre: string | null;
  tareaManual: string;
  cantidad: number | null;
  precioUnitario: number;
  unidad: string | null;
  aplicaIva: boolean;
  descuentoPorcentaje: number;
  descuentoFijo: number;
  visiblePdf: boolean;
  confianza: PresupuestoIaConfianza;
  faltaPrecio: boolean;
  cantidadDudosa: boolean;
}

export interface PresupuestoIaBorradorRequest {
  texto: string;
  clienteId?: number;
}

export type PresupuestoIaErrorKind =
  | 'disabled'
  | 'quota-hourly'
  | 'quota-daily'
  | 'quota-attempts'
  | 'quota-provider'
  | 'provider'
  | 'too-long'
  | 'forbidden'
  | 'unauthorized'
  | 'invalid-request'
  | 'unknown';

export class PresupuestoIaRequestError extends Error {
  constructor(
    readonly kind: PresupuestoIaErrorKind,
    readonly status: number | null,
    readonly serverMessage: string | null = null,
  ) {
    super(kind);
    this.name = 'PresupuestoIaRequestError';
  }
}
