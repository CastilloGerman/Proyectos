export interface Empresa {
  id?: number;
  nombre: string;
  direccion?: string;
  codigoPostal?: string;
  provincia?: string;
  pais?: string;
  nif?: string;
  telefono?: string;
  email?: string;
  notasPiePresupuesto?: string;
  notasPieFactura?: string;
  mailHost?: string;
  mailPort?: number;
  mailUsername?: string;
  mailPassword?: string;
  mailConfigurado?: boolean;
  tieneFirma?: boolean;
  firmaImagenBase64?: string | null;
  tieneLogo?: boolean;
  logoImagenBase64?: string | null;
  /** Valores por defecto en nuevas facturas (cobro a clientes). */
  defaultMetodoPago?: string | null;
  defaultCondicionesPago?: string | null;
  ibanCuenta?: string | null;
  bizumTelefono?: string | null;
  /** Facturación / régimen (también usado como defecto en nuevas facturas). */
  regimenIvaPrincipal?: string | null;
  descripcionActividadFiscal?: string | null;
  nifIntracomunitario?: string | null;
  epigrafeIae?: string | null;
  /** Solo métricas internas; no se muestra en facturas/PDF. */
  rubroAutonomoCodigo?: string | null;
  /** Recordatorios por email al cliente (facturas impagadas). */
  recordatorioClienteActivo?: boolean;
  /** Días tras el vencimiento (solo 7, 15, 30). */
  recordatorioClienteDias?: number[];
}
