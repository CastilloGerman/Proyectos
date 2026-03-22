/** Opciones para selects de país (ISO 3166-1 alpha-2 + nombre en español). */

function sortPaises(rows: { code: string; name: string }[]): { code: string; name: string }[] {
  return [...rows].sort((a, b) => a.name.localeCompare(b.name, 'es', { sensitivity: 'base' }));
}

/** Lista reducida si el runtime no expone `Intl.supportedValuesOf('region')`. */
const FALLBACK_PAISES_ES: { code: string; name: string }[] = [
  { code: 'ES', name: 'España' },
  { code: 'MX', name: 'México' },
  { code: 'AR', name: 'Argentina' },
  { code: 'CO', name: 'Colombia' },
  { code: 'PE', name: 'Perú' },
  { code: 'VE', name: 'Venezuela' },
  { code: 'CL', name: 'Chile' },
  { code: 'EC', name: 'Ecuador' },
  { code: 'GT', name: 'Guatemala' },
  { code: 'CU', name: 'Cuba' },
  { code: 'BO', name: 'Bolivia' },
  { code: 'DO', name: 'República Dominicana' },
  { code: 'HN', name: 'Honduras' },
  { code: 'PY', name: 'Paraguay' },
  { code: 'SV', name: 'El Salvador' },
  { code: 'NI', name: 'Nicaragua' },
  { code: 'CR', name: 'Costa Rica' },
  { code: 'PA', name: 'Panamá' },
  { code: 'UY', name: 'Uruguay' },
  { code: 'PR', name: 'Puerto Rico' },
  { code: 'US', name: 'Estados Unidos' },
  { code: 'FR', name: 'Francia' },
  { code: 'DE', name: 'Alemania' },
  { code: 'IT', name: 'Italia' },
  { code: 'PT', name: 'Portugal' },
  { code: 'GB', name: 'Reino Unido' },
  { code: 'BR', name: 'Brasil' },
  { code: 'MA', name: 'Marruecos' },
  { code: 'DZ', name: 'Argelia' },
  { code: 'RO', name: 'Rumanía' },
  { code: 'PL', name: 'Polonia' },
  { code: 'NL', name: 'Países Bajos' },
  { code: 'BE', name: 'Bélgica' },
  { code: 'CH', name: 'Suiza' },
  { code: 'AT', name: 'Austria' },
  { code: 'SE', name: 'Suecia' },
  { code: 'NO', name: 'Noruega' },
  { code: 'IE', name: 'Irlanda' },
];

/**
 * Construye la lista de países usando `Intl` cuando está disponible (nombres en español).
 */
export function buildPaisOptionsEs(): { code: string; name: string }[] {
  const intl = Intl as typeof Intl & { supportedValuesOf?: (key: string) => string[] };
  try {
    const codes = intl.supportedValuesOf?.('region');
    if (!codes?.length) {
      return sortPaises(FALLBACK_PAISES_ES);
    }
    const dn = new Intl.DisplayNames(['es'], { type: 'region' });
    const out: { code: string; name: string }[] = [];
    for (const c of codes) {
      if (!/^[A-Z]{2}$/.test(c)) {
        continue;
      }
      const name = dn.of(c);
      if (!name) {
        continue;
      }
      out.push({ code: c, name });
    }
    return sortPaises(out.length > 0 ? out : FALLBACK_PAISES_ES);
  } catch {
    return sortPaises(FALLBACK_PAISES_ES);
  }
}
