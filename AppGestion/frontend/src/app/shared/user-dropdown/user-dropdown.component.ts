import {
  Component,
  ElementRef,
  HostListener,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthService } from '../../core/auth/auth.service';
import { SubscriptionService } from '../../core/services/subscription.service';
import { environment } from '../../../environments/environment';
import { USER_MENU_SECTIONS, UserMenuItem } from './user-menu.config';
import { LogoutConfirmDialogComponent } from './logout-confirm-dialog.component';

@Component({
    selector: 'app-user-dropdown',
    imports: [RouterLink, MatIconModule, MatSnackBarModule],
    templateUrl: './user-dropdown.component.html',
    styleUrl: './user-dropdown.component.scss'
})
export class UserDropdownComponent {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly subscriptionService = inject(SubscriptionService);
  private readonly host = inject(ElementRef<HTMLElement>);

  readonly sections = USER_MENU_SECTIONS;
  readonly open = signal(false);

  private readonly panelRef = viewChild<ElementRef<HTMLElement>>('panel');

  /** Solo datos ya presentes en el usuario autenticado (sin token en UI). */
  readonly displayName = computed(() => {
    const u = this.auth.user();
    const name = u?.nombre?.trim();
    if (name) return name;
    const email = u?.email ?? '';
    const local = email.split('@')[0];
    return local || 'Usuario';
  });

  readonly displayInitials = computed(() => {
    const u = this.auth.user();
    const name = u?.nombre?.trim();
    if (name) {
      const parts = name.split(/\s+/).filter(Boolean);
      if (parts.length >= 2) {
        return (parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
      }
      return name.slice(0, 2).toUpperCase();
    }
    const email = u?.email ?? '';
    return email.length >= 2 ? email.slice(0, 2).toUpperCase() : '?';
  });

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.open()) return;
    const target = event.target as Node;
    if (this.host.nativeElement.contains(target)) return;
    this.close();
  }

  @HostListener('document:keydown', ['$event'])
  onDocumentKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape' && this.open()) {
      this.close();
    }
  }

  toggle(event: Event): void {
    event.stopPropagation();
    const next = !this.open();
    this.open.set(next);
    if (next) {
      queueMicrotask(() => this.focusFirstMenuitem());
    }
  }

  close(): void {
    this.open.set(false);
  }

  onAction(item: UserMenuItem, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    if (item.disabled || item.kind !== 'action' || !item.action) return;

    switch (item.action) {
      case 'subscription':
        this.openSubscriptionCheckout();
        break;
      case 'help-center':
        this.openHelpCenter();
        break;
      case 'contact-support':
        this.openContactSupport();
        break;
      default:
        break;
    }
    this.close();
  }

  confirmLogout(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.close();
    const ref = this.dialog.open(LogoutConfirmDialogComponent, {
      width: 'min(400px, 92vw)',
      autoFocus: 'dialog',
    });
    ref.afterClosed().subscribe((result) => {
      if (result === true) {
        this.auth.logout();
      }
    });
  }

  private focusFirstMenuitem(): void {
    const panel = this.panelRef()?.nativeElement;
    if (!panel) return;
    const first = panel.querySelector<HTMLElement>(
      'a[role="menuitem"], button[role="menuitem"]:not(.user-dropdown__logout)'
    );
    first?.focus();
  }

  private openSubscriptionCheckout(): void {
    this.subscriptionService.createCheckoutSession().subscribe({
      next: (res) => {
        if (res.checkoutUrl) {
          window.location.href = res.checkoutUrl;
        }
      },
      error: (err) => {
        this.snackBar.open(err.error?.error || 'Error al abrir el pago', 'Cerrar', { duration: 4000 });
      },
    });
  }

  private openHelpCenter(): void {
    const url = environment.helpCenterUrl?.trim();
    if (url) {
      window.open(url, '_blank', 'noopener,noreferrer');
    } else {
      void this.router.navigate(['/cuenta', 'centro-ayuda']);
    }
  }

  private openContactSupport(): void {
    const email = environment.supportEmail?.trim();
    if (email) {
      const subject = encodeURIComponent('Consulta desde Noemí / App gestión');
      window.location.href = `mailto:${email}?subject=${subject}`;
    } else {
      void this.router.navigate(['/cuenta', 'contactar-soporte']);
    }
  }
}
