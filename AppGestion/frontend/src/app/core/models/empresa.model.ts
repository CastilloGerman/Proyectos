export interface Empresa {
  id?: number;
  nombre: string;
  direccion?: string;
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
}
