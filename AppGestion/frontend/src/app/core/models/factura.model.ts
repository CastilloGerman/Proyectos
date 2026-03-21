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

export interface FacturaCobro {
  id: number;
  importe: number;
  fecha: string;
  metodo?: string;
  notas?: string;
  createdAt: string;
}

export interface FacturaCobroRequest {
  importe: number;
  fecha?: string;
  metodo?: string;
  notas?: string;
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
  montoCobrado?: number;
  notas?: string;
  items: FacturaItem[];
  /** URL de pago (Stripe Checkout) si se generó. */
  paymentLinkUrl?: string | null;
  cobros?: FacturaCobro[];
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
  montoCobrado?: number;
  notas?: string;
  ivaHabilitado?: boolean;
}
