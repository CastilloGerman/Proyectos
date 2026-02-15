export interface Cliente {
  id: number;
  nombre: string;
  telefono: string;
  email: string;
  direccion: string;
  dni: string;
  fechaCreacion: string;
}

export interface ClienteRequest {
  nombre: string;
  telefono?: string;
  email?: string;
  direccion?: string;
  dni?: string;
}
