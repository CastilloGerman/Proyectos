import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

const PATTERN = /^[A-Z]{2}[A-Z0-9]{2,12}$/i;

/** NIF-IVA intracomunitario (UE), p. ej. ESB12345678. Vacío = válido. */
export function nifIvaIntraValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const v = control.value;
    if (v == null || String(v).trim() === '') return null;
    const s = String(v).replace(/\s/g, '').toUpperCase();
    return PATTERN.test(s) ? null : { nifIvaIntraInvalido: true };
  };
}
