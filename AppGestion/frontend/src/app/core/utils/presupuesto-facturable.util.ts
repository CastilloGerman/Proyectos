/** Presupuesto que la API deja convertir en factura y que aún no tiene venta principal. */
export function presupuestoSePuedeFacturar(p: {
  estado?: string | null;
  facturaId?: number | null;
}): boolean {
  if (p.facturaId != null) return false;
  const e = (p.estado || '').trim().toLowerCase();
  return e === 'pendiente' || e === 'aceptado' || e === 'en ejecución' || e === 'en ejecucion';
}
