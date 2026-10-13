import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from './auth.service';
import { TranslateService } from '@ngx-translate/core';
import { environment } from '../../../environments/environment';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const snackBar = inject(MatSnackBar);
  const translate = inject(TranslateService);
  const token = auth.getToken();

  // SEC-07: el JWT solo viaja hacia nuestra API, nunca hacia terceros (CDN, Google, enlaces de pago...).
  const apiBase = environment.apiUrl;
  const isApiRequest = req.url === apiBase || req.url.startsWith(`${apiBase}/`) || req.url.startsWith(`${apiBase}?`);

  const cloned = token && isApiRequest
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(cloned).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401) {
        const url = req.url;
        const publicAuth =
          url.includes('/auth/login') ||
          url.includes('/auth/register') ||
          url.includes('/auth/google') ||
          url.includes('/auth/forgot-password') ||
          url.includes('/auth/reset-password') ||
          url.includes('/auth/verify-email') ||
          url.includes('/auth/resend-verification');
        const isLogoutRequest = url.includes('/auth/logout');
        const skipGlobalSessionHandler =
          publicAuth || isLogoutRequest || auth.isLogoutInProgress();
        if (!skipGlobalSessionHandler) {
          snackBar.open(translate.instant('shell.snackbarSessionExpired'), translate.instant('common.close'), {
            duration: 4000,
          });
          auth.clearSessionLocal();
          router.navigate(['/login']);
        }
      }
      return throwError(() => err);
    })
  );
};
