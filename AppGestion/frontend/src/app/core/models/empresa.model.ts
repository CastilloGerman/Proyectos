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
}
