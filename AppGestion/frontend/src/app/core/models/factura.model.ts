export interface FacturaItem {
  id?: number;
  materialId?: number;
  descripcion?: string;
  esTareaManual?: boolean;
  cantidad: number;
  precioUnitario: number;
  subtotal?: number;
}

export interface FacturaItemRequest {
  materialId?: number;
  tareaManual?: string;
  cantidad: number;
  precioUnitario: number;
  aplicaIva?: boolean;
}

export interface Factura {
  id: number;
  numeroFactura: string;
  clienteId: number;
  clienteNombre: string;
  presupuestoId?: number;
  fechaCreacion: string;
  fechaVencimiento?: string;
  subtotal: number;
  iva: number;
  total: number;
  ivaHabilitado: boolean;
  metodoPago: string;
  estadoPago: string;
  notas?: string;
  items: FacturaItem[];
}

export interface FacturaRequest {
  clienteId: number;
  presupuestoId?: number;
  items: FacturaItemRequest[];
  numeroFactura?: string;
  fechaVencimiento?: string;
  metodoPago?: string;
  estadoPago?: string;
  notas?: string;
  ivaHabilitado?: boolean;
}
