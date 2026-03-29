export type EstadoCliente = 'PROVISIONAL' | 'COMPLETO';

export interface Cliente {
  id: number;
  nombre: string;
  telefono: string;
  email: string;
  direccion: string;
  codigoPostal?: string;
  provincia?: string;
  pais?: string;
  dni: string;
  fechaCreacion: string;
  estadoCliente?: EstadoCliente;
}

export interface ClienteRequest {
  nombre: string;
  telefono?: string;
  email?: string;
  direccion?: string;
  codigoPostal?: string;
  provincia?: string;
  pais?: string;
  dni?: string;
}

export interface ClienteProvisionalRequest {
  nombre: string;
}

export interface ClienteCompletoRequest {
  nombre?: string;
  dni: string;
  direccion: string;
  codigoPostal: string;
  telefono?: string;
  email?: string;
  pais?: string;
  provincia?: string;
}
