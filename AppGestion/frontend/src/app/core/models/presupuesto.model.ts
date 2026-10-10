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
  /** PROVISIONAL | COMPLETO — desde API para facturación sin GET extra. */
  clienteEstado?: string | null;
  clienteEmail?: string;
  clienteTelefono?: string;
  clientePais?: string;
  enviadoAt?: string | null;
  canalEnvio?: 'WHATSAPP' | 'EMAIL' | null;
  enlacePrimeraVistaAt?: string | null;
  enlaceNumVistas?: number;
  seguimientoAvisosEnviados?: number;
  seguimientoUltimoAvisoAt?: string | null;
  seguimientoSilenciado?: boolean;
  respuestaCliente?: 'INTERESA' | 'DUDAS' | null;
  respuestaClienteAt?: string | null;
  respuestaClienteMensaje?: string | null;
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
  /** Claves de condiciones predefinidas activas (catálogo en servidor). */
  condicionesActivas?: string[] | null;
  /** Texto libre opcional al pie del PDF. */
  notaAdicional?: string | null;
  /** Anticipo fiscal registrado (antes de facturar el anticipo). */
  tieneAnticipo?: boolean;
  importeAnticipo?: number | null;
  anticipoFacturado?: boolean;
  fechaAnticipo?: string | null;
  items: PresupuestoItem[];
}

export interface PresupuestoEnlaceCreado {
  url: string;
  expiraAt: string;
}

export interface PresupuestoEnlaceEstado {
  activo: boolean;
  expiraAt: string | null;
  primeraVistaAt: string | null;
  ultimaVistaAt: string | null;
  numVistas: number;
  enlacesActivos: number;
}

export interface PresupuestoPublico {
  empresaNombre: string | null;
  empresaLogoBase64: string | null;
  empresaLogoMimeType: string | null;
  numero: number;
  fecha: string;
  clienteNombre: string;
  partidas: Array<{ descripcion: string | null; cantidad: number; unidad: string; precioUnitario: number; subtotal: number }>;
  subtotal: number;
  iva: number;
  total: number;
  notas: string | null;
  condiciones: string[];
  permiteResponder: boolean;
  respuestaCliente?: 'INTERESA' | 'DUDAS' | null;
}

export type PresupuestoRespuestaClienteOpcion = 'INTERESA' | 'DUDAS';

export interface PresupuestoRespuestaClienteRequest {
  opcion: PresupuestoRespuestaClienteOpcion;
  mensaje?: string;
}

export interface PresupuestoRequest {
  clienteId: number;
  items: PresupuestoItemRequest[];
  ivaHabilitado?: boolean;
  estado?: string;
  descuentoGlobalPorcentaje?: number;
  descuentoGlobalFijo?: number;
  descuentoAntesIva?: boolean;
  condicionesActivas?: string[];
  notaAdicional?: string;
}

/** Registro de anticipo (POST /presupuestos/:id/anticipo). */
export interface AnticipoRegistroRequest {
  importeAnticipo: number;
  fechaAnticipo: string;
}

/** Resumen de importes del flujo de anticipo (GET resumen-anticipo). */
export interface AnticipoResumen {
  totalPresupuesto: number;
  importeAnticipo: number;
  baseAnticipo: number;
  ivaAnticipo: number;
  importePendiente: number;
  basePendiente: number;
  ivaPendiente: number;
  anticipoYaFacturado: boolean;
  tieneAnticipoRegistrado: boolean;
}
