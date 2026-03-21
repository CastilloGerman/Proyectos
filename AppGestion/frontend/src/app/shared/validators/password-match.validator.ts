import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** La contraseña debe coincidir con el control hermano `matchKey` (p. ej. `newPassword`). */
export function passwordMatchValidator(matchKey: string): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) return null;
    const parent = control.parent;
    if (!parent) return null;
    const other = parent.get(matchKey);
    if (!other) return null;
    return control.value === other.value ? null : { mismatch: true };
  };
}
