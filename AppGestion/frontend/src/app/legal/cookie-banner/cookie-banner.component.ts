import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CookieConsentService } from '../../core/consent/cookie-consent.service';

@Component({
  selector: 'app-cookie-banner',
  standalone: true,
  imports: [RouterLink],
  template: `
    @if (consent.status() === null) {
      <div class="cookie-banner" role="dialog" aria-label="Consentimiento de cookies">
        <p>
          Usamos cookies necesarias para el funcionamiento del servicio y,
          con tu consentimiento, cookies analíticas para mejorar Noemi.
          <a routerLink="/cookies">Más información</a>
        </p>
        <button type="button" class="btn-reject" (click)="consent.reject()">Rechazar</button>
        <button type="button" class="btn-accept" (click)="consent.accept()">Aceptar</button>
      </div>
    }
  `,
})
export class CookieBannerComponent {
  readonly consent = inject(CookieConsentService);
}
