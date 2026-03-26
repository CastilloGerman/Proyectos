import { Cliente } from './cliente.model';

export interface ClienteFacturaResumen {
  id: number;
  numeroFactura: string;
  fechaCreacion: string;
  fechaExpedicion?: string;
  fechaVencimiento?: string;
  total: number;
  estadoPago: string;
  montoCobrado?: number;
  pendiente: number;
}

export interface ClientePresupuestoResumen {
  id: number;
  fechaCreacion: string;
  total: number;
  estado: string;
  facturaId: number | null;
  activo: boolean;
}

export interface ClienteHistorialItem {
  tipo: 'FACTURA' | 'PRESUPUESTO';
  id: number;
  fechaOrden: string;
  etiqueta: string;
  importe: number;
  estado: string;
  subetiqueta: string;
}

export interface ClientePanel {
  cliente: Cliente;
  totalPendienteCobro: number;
  facturasConPendiente: number;
  facturas: ClienteFacturaResumen[];
  presupuestos: ClientePresupuestoResumen[];
  presupuestosActivos: ClientePresupuestoResumen[];
  historial: ClienteHistorialItem[];
}
