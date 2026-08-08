/** Tipo IVA general aplicado en presupuestos (21 %). */
export const PRESUPUESTO_IVA_RATE = 0.21;

export interface PresupuestoCostesItemInput {
  cantidad: number;
  precioUnitario: number;
  descuentoPorcentaje?: number;
  descuentoFijo?: number;
  aplicaIva?: boolean;
}

export interface PresupuestoCostesInput {
  items: PresupuestoCostesItemInput[];
  descuentoGlobalPorcentaje?: number;
  descuentoGlobalFijo?: number;
  descuentoAntesIva?: boolean;
  ivaHabilitado?: boolean;
}

export interface PresupuestoCostesResumen {
  subtotalItems: number;
  descuentoPorcentaje: number;
  descuentoFijo: number;
  descuentoTotal: number;
  baseIva: number;
  iva: number;
  total: number;
}

/**
 * Calcula subtotal, descuentos, base IVA y total de un presupuesto.
 * Misma lógica que el formulario de presupuesto (preview antes de guardar).
 */
export function calcularPresupuestoCostes(input: PresupuestoCostesInput): PresupuestoCostesResumen {
  let subtotalItems = 0;
  let baseIva = 0;

  for (const item of input.items) {
    const cantidad = +(item.cantidad ?? 0);
    const precio = +(item.precioUnitario ?? 0);
    const descPct = +(item.descuentoPorcentaje ?? 0);
    const descFijo = +(item.descuentoFijo ?? 0);
    let itemSub = cantidad * precio;
    itemSub = itemSub * (1 - descPct / 100) - descFijo;
    itemSub = Math.max(0, itemSub);
    subtotalItems += itemSub;
    if (item.aplicaIva !== false) {
      baseIva += itemSub;
    }
  }

  const descPct = +(input.descuentoGlobalPorcentaje ?? 0);
  const descFijo = +(input.descuentoGlobalFijo ?? 0);
  const descuentoAntesIva = input.descuentoAntesIva !== false;
  let subtotal = subtotalItems;

  if (descuentoAntesIva) {
    subtotal = subtotal * (1 - descPct / 100) - descFijo;
    baseIva = baseIva * (1 - descPct / 100) - descFijo;
  } else {
    subtotal = subtotal * (1 - descPct / 100) - descFijo;
  }

  subtotal = Math.max(0, subtotal);
  baseIva = Math.max(0, baseIva);
  const descuentoTotal = subtotalItems - subtotal;
  const ivaHabilitado = input.ivaHabilitado !== false;
  const iva = ivaHabilitado ? baseIva * PRESUPUESTO_IVA_RATE : 0;
  const total = subtotal + iva;

  return {
    subtotalItems,
    descuentoPorcentaje: descPct,
    descuentoFijo: descFijo,
    descuentoTotal,
    baseIva,
    iva,
    total,
  };
}
