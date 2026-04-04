/** Por debajo de este umbral (inclusive) se muestra el aviso de fin de prueba en el layout autenticado. */
export const TRIAL_BANNER_WARNING_DAYS = 7;

/**
 * Valor devuelto cuando no hay fecha de fin de trial (no mostrar banner) o cálculo inválido.
 * Debe ser mayor que {@link TRIAL_BANNER_WARNING_DAYS}.
 */
export const TRIAL_DAYS_LEFT_FALLBACK = 99;
