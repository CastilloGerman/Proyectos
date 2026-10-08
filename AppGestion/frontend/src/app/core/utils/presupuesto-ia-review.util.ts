import { PresupuestoIaConfianza } from '../models/presupuesto-ia.model';

export interface PresupuestoIaRevisionDatos {
  iaSugerida: boolean;
  iaRevisada: boolean;
  confianza: PresupuestoIaConfianza | string;
  materialId: unknown;
  materialEnCatalogo: boolean;
  faltaPrecio: boolean;
  cantidadDudosa: boolean;
  cantidad: unknown;
  precioUnitario: unknown;
}

export interface PresupuestoIaPendientes {
  precio: number;
  cantidad: number;
  revision: number;
}

export function parsePresupuestoDecimal(value: unknown): number | null {
  if (typeof value === 'number') return Number.isFinite(value) ? value : null;
  if (typeof value !== 'string') return null;
  const raw = value.trim();
  if (!/^[+-]?(?:\d+(?:[.,]\d*)?|[.,]\d+)$/.test(raw)) return null;
  const parsed = Number(raw.replace(',', '.'));
  return Number.isFinite(parsed) ? parsed : null;
}

export function esCantidadPrecioValido(cantidad: unknown, precio: unknown): boolean {
  const parsedCantidad = parsePresupuestoDecimal(cantidad);
  const parsedPrecio = parsePresupuestoDecimal(precio);
  return parsedCantidad != null && parsedCantidad >= 0.001 && parsedPrecio != null && parsedPrecio >= 0;
}

export function evaluarPendientesPresupuestoIa(items: PresupuestoIaRevisionDatos[]): PresupuestoIaPendientes {
  return items.reduce<PresupuestoIaPendientes>((pending, item) => {
    if (!item.iaSugerida) return pending;
    if (!item.iaRevisada) pending.revision++;
    const price = parsePresupuestoDecimal(item.precioUnitario);
    const quantity = parsePresupuestoDecimal(item.cantidad);
    if (item.faltaPrecio || price == null || price <= 0) pending.precio++;
    if (item.cantidadDudosa || quantity == null || quantity <= 0) pending.cantidad++;
    return pending;
  }, { precio: 0, cantidad: 0, revision: 0 });
}

export function esSugerenciaPresupuestoIaSegura(item: PresupuestoIaRevisionDatos): boolean {
  const price = parsePresupuestoDecimal(item.precioUnitario);
  return item.iaSugerida && !item.iaRevisada &&
    item.confianza === 'alta' &&
    typeof item.materialId === 'number' && item.materialEnCatalogo &&
    !item.faltaPrecio && !item.cantidadDudosa &&
    price != null && price > 0 &&
    esCantidadPrecioValido(item.cantidad, item.precioUnitario);
}
