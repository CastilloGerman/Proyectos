const TIPOS_IVA_LINEA = [21, 10, 4, 0] as const;

export function esTipoIvaLinea(valor: number): boolean {
  return (TIPOS_IVA_LINEA as readonly number[]).includes(valor);
}

/**
 * Tipo al volver a marcar el IVA de la factura.
 * Si la línea tenía 21, 10, 4 o 0, se recupera ese tipo.
 * Si no hay tipo recordado y la línea estaba deshabilitada (se añadió con el IVA desmarcado), el defecto es 21.
 * Devuelve null cuando el IVA ya estaba marcado y no hay que pisar el tipo cargado.
 */
export function tipoAlVolverAMarcarIva(
  recordado: number | undefined,
  lineaEstabaDeshabilitada: boolean,
): number | null {
  if (recordado != null && esTipoIvaLinea(recordado)) {
    return recordado;
  }
  if (recordado != null || lineaEstabaDeshabilitada) {
    return 21;
  }
  return null;
}

/** Recuerda el tipo de cada línea mientras el IVA de la factura está desmarcado. */
export class MemoriaTipoIva {
  private readonly recordados = new WeakMap<object, number>();

  /** Guarda el tipo actual y devuelve 0, que es lo que muestra la línea con el IVA apagado. */
  apagar(linea: object, tipoActual: number): number {
    if (!this.recordados.has(linea)) {
      this.recordados.set(linea, esTipoIvaLinea(tipoActual) ? tipoActual : 21);
    }
    return 0;
  }

  encender(linea: object, lineaEstabaDeshabilitada: boolean): number | null {
    const tenia = this.recordados.has(linea);
    const recordado = this.recordados.get(linea);
    if (tenia) {
      this.recordados.delete(linea);
    }
    return tipoAlVolverAMarcarIva(tenia ? recordado : undefined, lineaEstabaDeshabilitada);
  }
}
