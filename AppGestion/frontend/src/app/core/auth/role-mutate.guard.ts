import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from './auth.service';

/** Impide acceso a rutas de alta/edición si no hay permiso de escritura (suscripción / prueba). */
export const roleMutateGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.canMutate()) {
    return true;
  }
  router.navigate(['/dashboard']);
  return false;
};
