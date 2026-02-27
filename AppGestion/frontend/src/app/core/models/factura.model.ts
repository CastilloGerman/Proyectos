export interface FacturaItem {
  id?: number;
  materialId?: number;
  descripcion?: string;
  esTareaManual?: boolean;
  cantidad: number;
  precioUnitario: number;
  subtotal?: number;
  aplicaIva?: boolean;
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
  clienteEmail?: string;
  presupuestoId?: number;
  fechaCreacion: string;
  fechaExpedicion?: string;
  fechaOperacion?: string;
  fechaVencimiento?: string;
  subtotal: number;
  iva: number;
  total: number;
  ivaHabilitado: boolean;
  regimenFiscal?: string;
  condicionesPago?: string;
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
  fechaExpedicion?: string;
  fechaOperacion?: string;
  fechaVencimiento?: string;
  regimenFiscal?: string;
  condicionesPago?: string;
  metodoPago?: string;
  estadoPago?: string;
  notas?: string;
  ivaHabilitado?: boolean;
}
