export interface PresupuestoItem {
  id?: number;
  materialId?: number;
  descripcion?: string;
  esTareaManual?: boolean;
  cantidad: number;
  precioUnitario: number;
  subtotal?: number;
  visiblePdf?: boolean;
}

export interface PresupuestoItemRequest {
  materialId?: number;
  tareaManual?: string;
  cantidad: number;
  precioUnitario: number;
  aplicaIva?: boolean;
  descuentoPorcentaje?: number;
  descuentoFijo?: number;
  visiblePdf?: boolean;
}

export interface Presupuesto {
  id: number;
  clienteId: number;
  clienteNombre: string;
  clienteEmail?: string;
  fechaCreacion: string;
  subtotal: number;
  iva: number;
  total: number;
  ivaHabilitado: boolean;
  estado: string;
  /** Si ya existe factura generada desde este presupuesto. */
  facturaId?: number | null;
  descuentoGlobalPorcentaje?: number;
  descuentoGlobalFijo?: number;
  descuentoAntesIva?: boolean;
  /** Texto extra en PDF con variables {{cliente.nombre}}, {{total}}, etc. */
  textoClausulas?: string | null;
  /** Importe de señal / anticipo acordado. */
  senalImporte?: number | null;
  /** Si la señal ya fue cobrada. */
  senalPagada?: boolean;
  items: PresupuestoItem[];
}

export interface PresupuestoRequest {
  clienteId: number;
  items: PresupuestoItemRequest[];
  ivaHabilitado?: boolean;
  estado?: string;
  descuentoGlobalPorcentaje?: number;
  descuentoGlobalFijo?: number;
  descuentoAntesIva?: boolean;
  textoClausulas?: string;
  senalImporte?: number;
  senalPagada?: boolean;
}
