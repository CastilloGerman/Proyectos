/** Tolerancia en euros para comparar importes cobrados vs total. */
export const FACTURA_COBRO_EPSILON = 0.009;

export interface FacturaCobroSnapshot {
  estadoPago?: string | null;
  total?: number | null;
  montoCobrado?: number | null;
}

/** Importe pendiente de cobro (0 si ya está pagada por completo). */
export function calcularImportePendiente(f: FacturaCobroSnapshot): number {
  const total = +(f.total ?? 0);
  if (total <= 0) return 0;
  const estado = (f.estadoPago ?? '').toLowerCase();
  if (estado === 'pagada') return 0;
  if (estado === 'parcial') {
    const cobrado = +(f.montoCobrado ?? 0);
    return Math.max(0, total - cobrado);
  }
  return total;
}

/** True si la factura aún tiene importe pendiente de cobro. */
export function facturaTieneImportePendiente(f: FacturaCobroSnapshot): boolean {
  return calcularImportePendiente(f) > FACTURA_COBRO_EPSILON;
}

/** True si el importe parcial es válido: > 0 y estrictamente menor que el total. */
export function esImporteParcialValido(importe: number, totalFactura: number): boolean {
  const total = +totalFactura;
  const v = +importe;
  if (!Number.isFinite(total) || total <= 0) return false;
  if (!Number.isFinite(v) || v <= 0) return false;
  if (v > total + FACTURA_COBRO_EPSILON) return false;
  if (v >= total - FACTURA_COBRO_EPSILON) return false;
  return true;
}
