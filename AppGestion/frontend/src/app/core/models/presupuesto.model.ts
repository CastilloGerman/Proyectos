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
  fechaCreacion: string;
  subtotal: number;
  iva: number;
  total: number;
  ivaHabilitado: boolean;
  estado: string;
  descuentoGlobalPorcentaje?: number;
  descuentoGlobalFijo?: number;
  descuentoAntesIva?: boolean;
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
}
