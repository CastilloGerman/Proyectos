import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Quita espacios, puntos y guiones; deja solo letras y números para validar/guardar. */
export function normalizarIbanParaValidar(raw: string | null | undefined): string {
  return String(raw ?? '')
    .replace(/[\s.\u00A0\-]/g, '')
    .toUpperCase();
}

/** Comprueba IBAN (mod 97, ISO 13616). Vacío = válido (opcional). Acepta espacios en la entrada. */
export function esIbanValido(raw: string | null | undefined): boolean {
  if (raw == null || String(raw).trim() === '') return true;
  const iban = normalizarIbanParaValidar(raw);
  if (iban.length < 15 || iban.length > 34 || !/^[A-Z0-9]+$/.test(iban)) return false;
  const rearranged = iban.slice(4) + iban.slice(0, 4);
  let expanded = '';
  for (const c of rearranged) {
    if (c >= '0' && c <= '9') expanded += c;
    else if (c >= 'A' && c <= 'Z') expanded += String(c.charCodeAt(0) - 55);
    else return false;
  }
  try {
    return BigInt(expanded) % 97n === 1n;
  } catch {
    return false;
  }
}

export function ibanValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const v = control.value;
    if (v == null || String(v).trim() === '') return null;
    return esIbanValido(v) ? null : { ibanInvalido: true };
  };
}

/**
 * Formato legible para mostrar en pantalla/PDF.
 * España (24 caracteres ES + 22 dígitos): ESkk BBBB GGGG CC CCCCCCCCCC (como en cartillas bancarias).
 * Resto de países: grupos de 4.
 */
export function formatIbanDisplay(raw: string | null | undefined): string {
  if (!raw) return '';
  const s = normalizarIbanParaValidar(raw);
  if (s.length === 0) return '';
  if (s.startsWith('ES') && s.length === 24 && /^ES\d{22}$/.test(s)) {
    return [s.slice(0, 4), s.slice(4, 8), s.slice(8, 12), s.slice(12, 14), s.slice(14, 24)].join(' ');
  }
  return s.replace(/(.{4})/g, '$1 ').trim();
}
