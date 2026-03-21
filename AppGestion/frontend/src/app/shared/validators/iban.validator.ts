import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Comprueba IBAN (mod 97). Vacío = válido (opcional). */
export function esIbanValido(raw: string | null | undefined): boolean {
  if (raw == null || String(raw).trim() === '') return true;
  const iban = String(raw).replace(/\s/g, '').toUpperCase();
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

/** Formato legible ES12 3456 7890 … */
export function formatIbanDisplay(raw: string | null | undefined): string {
  if (!raw) return '';
  const s = String(raw).replace(/\s/g, '').toUpperCase();
  return s.replace(/(.{4})/g, '$1 ').trim();
}
