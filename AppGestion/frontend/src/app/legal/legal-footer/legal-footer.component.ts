import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../core/auth/auth.service';
import { isLegalPath } from '../legal-paths';

@Component({
  selector: 'app-legal-footer',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule],
  template: `
    <footer class="legal-footer">
      @if (showBackToHome()) {
        <div class="legal-back-home">
          <a mat-stroked-button [routerLink]="backToHomeLink()" class="legal-back-home-btn">
            <mat-icon>arrow_back</mat-icon>
            Volver al inicio
          </a>
        </div>
      }
      <nav class="footer-legal" aria-label="Legal">
        <a routerLink="/privacidad">Política de privacidad</a>
        <a routerLink="/terminos">Términos y condiciones</a>
        <a routerLink="/cookies">Política de cookies</a>
        <a routerLink="/aviso-legal">Aviso legal</a>
        <a routerLink="/reembolsos">Reembolsos</a>
      </nav>
    </footer>
  `,
  styles: [
    `
      .legal-footer {
        max-width: 800px;
        margin: 0 auto;
      }

      .legal-back-home {
        display: flex;
        justify-content: center;
        padding: 32px 24px 8px;
      }

      .legal-back-home-btn mat-icon {
        margin-right: 4px;
      }
    `,
  ],
})
export class LegalFooterComponent {
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  readonly showBackToHome = computed(() => isLegalPath(this.router.url));
  readonly backToHomeLink = computed(() => (this.auth.isAuthenticated() ? '/dashboard' : '/login'));
}
