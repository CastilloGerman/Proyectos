import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-cookie-banner',
  standalone: true,
  imports: [RouterLink],
  template: `
    @if (showBanner) {
      <div class="cookie-banner" role="dialog" aria-label="Consentimiento de cookies">
        <p>
          Usamos cookies necesarias para el funcionamiento del servicio y,
          con tu consentimiento, cookies analíticas para mejorar Noemi.
          <a routerLink="/cookies">Más información</a>
        </p>
        <button type="button" class="btn-accept" (click)="accept()">Aceptar</button>
      </div>
    }
  `,
})
export class CookieBannerComponent implements OnInit {
  showBanner = false;

  ngOnInit(): void {
    this.showBanner = !localStorage.getItem('cookie_consent');
  }

  accept(): void {
    localStorage.setItem('cookie_consent', 'accepted');
    this.showBanner = false;
  }
}
