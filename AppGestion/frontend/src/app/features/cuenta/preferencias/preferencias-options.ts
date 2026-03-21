/** Opciones mostradas en UI; el API acepta cualquier zona IANA válida si se ampliara el selector. */

export const LOCALE_OPTIONS = [
  { value: 'es', label: 'Español' },
  { value: 'en', label: 'English' },
] as const;

export const CURRENCY_OPTIONS = [
  { value: 'EUR', label: 'Euro (EUR)' },
  { value: 'USD', label: 'Dólar estadounidense (USD)' },
  { value: 'GBP', label: 'Libra esterlina (GBP)' },
  { value: 'MXN', label: 'Peso mexicano (MXN)' },
  { value: 'COP', label: 'Peso colombiano (COP)' },
  { value: 'ARS', label: 'Peso argentino (ARS)' },
  { value: 'CLP', label: 'Peso chileno (CLP)' },
] as const;

/** Zonas habituales para autónomos en España y Latinoamérica (+ US/UK). */
export const TIMEZONE_OPTIONS = [
  { value: 'Europe/Madrid', label: 'España (península y Baleares)' },
  { value: 'Atlantic/Canary', label: 'España (Canarias)' },
  { value: 'Europe/Lisbon', label: 'Portugal' },
  { value: 'Europe/Paris', label: 'Francia / Alemania / Italia (CET)' },
  { value: 'Europe/London', label: 'Reino Unido / Irlanda' },
  { value: 'America/Mexico_City', label: 'México (centro)' },
  { value: 'America/Bogota', label: 'Colombia / Perú / Ecuador' },
  { value: 'America/Caracas', label: 'Venezuela' },
  { value: 'America/Lima', label: 'Perú (Lima)' },
  { value: 'America/Santiago', label: 'Chile' },
  { value: 'America/Argentina/Buenos_Aires', label: 'Argentina' },
  { value: 'America/Montevideo', label: 'Uruguay' },
  { value: 'America/Sao_Paulo', label: 'Brasil (este)' },
  { value: 'America/New_York', label: 'Este de EE. UU.' },
  { value: 'America/Chicago', label: 'Centro de EE. UU.' },
  { value: 'America/Denver', label: 'Montaña (EE. UU.)' },
  { value: 'America/Los_Angeles', label: 'Pacífico (EE. UU.)' },
  { value: 'UTC', label: 'UTC (sin ajuste local)' },
] as const;
