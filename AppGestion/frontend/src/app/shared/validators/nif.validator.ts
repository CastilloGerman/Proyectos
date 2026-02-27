import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

const DNI_LETRAS = 'TRWAGMYFPDXBNJZSQVHLCKE';
const CIF_LETRAS_CONTROL = 'JABCDEFGHI';
const CIF_CALCULO = [0, 2, 4, 6, 8, 1, 3, 5, 7, 9];

function normalizar(nif: string): string {
  return (nif || '').replace(/[\s\-]/g, '').toUpperCase();
}

function esDni(n: string): boolean {
  return /^\d{8}[A-Z]$/.test(n);
}

function esNie(n: string): boolean {
  return /^[XYZ]\d{7}[A-Z]$/.test(n);
}

function esCif(n: string): boolean {
  return /^[A-HJ-NP-SUVW]\d{7}[0-9A-J]$/.test(n);
}

function validarDni(dni: string): boolean {
  const num = parseInt(dni.substring(0, 8), 10);
  const letra = dni.charAt(8);
  return DNI_LETRAS.charAt(num % 23) === letra;
}

function validarNie(nie: string): boolean {
  const digitoInicial = nie.charAt(0) === 'X' ? 0 : nie.charAt(0) === 'Y' ? 1 : 2;
  const num = digitoInicial * 10000000 + parseInt(nie.substring(1, 8), 10);
  const letra = nie.charAt(8);
  return DNI_LETRAS.charAt(num % 23) === letra;
}

function validarCif(cif: string): boolean {
  const sieteDigitos = cif.substring(1, 8);
  let a = 0;
  let b = 0;
  for (let i = 0; i < 7; i++) {
    const d = parseInt(sieteDigitos.charAt(i), 10);
    if (i % 2 === 0) {
      b += CIF_CALCULO[d];
    } else {
      a += d;
    }
  }
  const c = a + b;
  const d = (10 - (c % 10)) % 10;
  const control = cif.charAt(8);
  if (/^\d$/.test(control)) {
    return parseInt(control, 10) === d;
  }
  return CIF_LETRAS_CONTROL.charAt(d) === control;
}

/**
 * Valida si el NIF es correcto (DNI, NIE o CIF español).
 */
export function esNifValido(nif: string | null | undefined): boolean {
  if (!nif || typeof nif !== 'string' || nif.trim() === '') {
    return true; // vacío = válido (usar required para obligatorio)
  }
  const n = normalizar(nif);
  if (n.length !== 9) return false;
  if (esCif(n)) return validarCif(n);
  if (esNie(n)) return validarNie(n);
  if (esDni(n)) return validarDni(n);
  return false;
}

/**
 * Validador Angular para NIF (DNI/NIE/CIF).
 * Si el campo está vacío, no marca error (combinar con Validators.required si es obligatorio).
 */
export function nifValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;
    if (!value || (typeof value === 'string' && value.trim() === '')) {
      return null;
    }
    return esNifValido(value) ? null : { nifInvalido: { value: control.value } };
  };
}
