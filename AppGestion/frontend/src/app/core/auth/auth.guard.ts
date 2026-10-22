import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslateService } from '@ngx-translate/core';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isAuthenticated()) {
    return true;
  }

  if (auth.consumeSessionExpiredOnLoad?.()) {
    const snackBar = inject(MatSnackBar);
    const translate = inject(TranslateService);
    snackBar.open(translate.instant('shell.snackbarSessionExpired'), translate.instant('common.close'), {
      duration: 4000,
    });
  }

  router.navigate(['/login']);
  return false;
};
