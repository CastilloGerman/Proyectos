import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-legal-footer',
  standalone: true,
  imports: [RouterLink],
  template: `
    <nav class="footer-legal" aria-label="Legal">
      <a routerLink="/privacidad">Política de privacidad</a>
      <a routerLink="/terminos">Términos y condiciones</a>
      <a routerLink="/cookies">Política de cookies</a>
      <a routerLink="/aviso-legal">Aviso legal</a>
      <a routerLink="/reembolsos">Reembolsos</a>
    </nav>
  `,
})
export class LegalFooterComponent {}
