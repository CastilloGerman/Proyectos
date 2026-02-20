export interface Material {
  id: number;
  nombre: string;
  precioUnitario: number;
  unidadMedida: string;
}

export interface MaterialRequest {
  nombre: string;
  precioUnitario: number;
  unidadMedida?: string;
}
