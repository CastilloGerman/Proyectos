import { Observable, debounceTime, switchMap } from 'rxjs';
import { FiscalCriterio } from '../../../core/models/fiscal.model';

/** Selección con la que se pide el resumen. Se captura al cambiar el filtro, no al llegar la respuesta. */
export interface FiltroModelo303 {
  year: number;
  trimestre: number;
  criterio: FiscalCriterio;
  soloPagadas: boolean;
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
