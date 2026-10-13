import { Observable, debounceTime, switchMap } from 'rxjs';
import { FiscalCriterio } from '../../../core/models/fiscal.model';

/** Selección con la que se pide el resumen. Se captura al cambiar el filtro, no al llegar la respuesta. */
export interface FiltroModelo303 {
  year: number;
  trimestre: number;
  criterio: FiscalCriterio;
  soloPagadas: boolean;
}

export const ETIQUETA_SIN_DATOS = 'Sin datos en este trimestre';
export const ETIQUETA_A_INGRESAR = 'IVA estimado a ingresar';
export const ETIQUETA_A_COMPENSAR = 'IVA estimado a compensar';

/**
 * Cero facturas y cero gastos no pintan 0,00 €. Una factura de 0 €, o un trimestre
 * en el que el repercutido iguala al soportado, sí: «IVA estimado a ingresar» y importe 0.
 */
export function cifraModelo303(
  numeroFacturas: number,
  numeroGastos: number,
  resultadoIva: number,
): { etiqueta: string; importe: number | null } {
  if ((numeroFacturas ?? 0) === 0 && (numeroGastos ?? 0) === 0) {
    return { etiqueta: ETIQUETA_SIN_DATOS, importe: null };
  }
  const resultado = resultadoIva ?? 0;
  if (resultado < 0) {
    return { etiqueta: ETIQUETA_A_COMPENSAR, importe: Math.abs(resultado) };
  }
  return { etiqueta: ETIQUETA_A_INGRESAR, importe: Math.abs(resultado) };
}

/**
 * Espera a que el usuario deje quietos los selectores y cancela la petición anterior.
 * Una respuesta tardía del trimestre que ya no está seleccionado no se emite.
 */
export function observacionesModelo303<T>(
  filtros: Observable<FiltroModelo303>,
  cargar: (filtro: FiltroModelo303) => Observable<T>,
  esperaMs = 300,
): Observable<T> {
  return filtros.pipe(
    debounceTime(esperaMs),
    switchMap((filtro) => cargar(filtro)),
  );
}
