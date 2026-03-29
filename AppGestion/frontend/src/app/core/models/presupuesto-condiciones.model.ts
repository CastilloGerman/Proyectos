/** Catálogo servido por GET /presupuestos/condiciones-disponibles (texto solo en servidor). */
export interface PresupuestoCondicionDisponible {
  clave: string;
  textoVisible: string;
  activaPorDefecto: boolean;
}

/**
 * Valor del bloque "condiciones + nota" en formularios.
 * Se serializa en PresupuestoRequest como condicionesActivas + notaAdicional.
 */
export interface CondicionesPresupuestoFormValue {
  condicionesActivas: string[];
  notaAdicional: string;
}
