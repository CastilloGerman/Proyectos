import { Component, OnInit, inject, signal, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import {
  FiltroLectura,
  NotificacionDto,
  NotificacionesService,
} from '../../../core/services/notificaciones.service';

@Component({
  selector: 'app-notificaciones',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDividerModule,
  ],
  templateUrl: './notificaciones.component.html',
  styleUrl: './notificaciones.component.scss',
})
export class NotificacionesComponent implements OnInit {
  readonly notifSvc = inject(NotificacionesService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly items = signal<NotificacionDto[]>([]);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly pageIndex = signal(0);
  readonly pageSize = 20;

  readonly filtro = new FormControl<FiltroLectura>('todas', { nonNullable: true });

  constructor() {
    this.filtro.valueChanges
      .pipe(debounceTime(0), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.pageIndex.set(0);
        this.load();
      });
  }

  ngOnInit(): void {
    this.notifSvc.refreshUnreadCount();
    this.load();
  }

  load(): void {
    this.loading.set(true);
    const f = this.filtro.value ?? 'todas';
    this.notifSvc.list(this.pageIndex(), this.pageSize, f).subscribe({
      next: (p) => {
        this.items.set(p.content ?? []);
        this.totalElements.set(p.totalElements ?? 0);
        this.totalPages.set(p.totalPages ?? 0);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('No se pudieron cargar las notificaciones', 'Cerrar', { duration: 5000 });
      },
    });
  }

  prevPage(): void {
    if (this.pageIndex() > 0) {
      this.pageIndex.update((n) => n - 1);
      this.load();
    }
  }

  nextPage(): void {
    if (this.pageIndex() < this.totalPages() - 1) {
      this.pageIndex.update((n) => n + 1);
      this.load();
    }
  }

  marcarTodasLeidas(): void {
    if (this.notifSvc.unreadCount() === 0) return;
    this.saving.set(true);
    this.notifSvc.markAllRead().subscribe({
      next: () => {
        this.saving.set(false);
        this.snackBar.open('Notificaciones marcadas como leídas', 'Cerrar', { duration: 3000 });
        this.load();
      },
      error: () => {
        this.saving.set(false);
        this.snackBar.open('No se pudo completar la acción', 'Cerrar', { duration: 4000 });
      },
    });
  }

  abrir(n: NotificacionDto): void {
    if (!n.leida) {
      this.notifSvc.markRead(n.id).subscribe({
        next: () => {
          this.items.update((list) => list.map((x) => (x.id === n.id ? { ...x, leida: true } : x)));
        },
      });
    }
    const path = n.actionPath?.trim();
    if (path && path.startsWith('/') && !path.startsWith('//')) {
      void this.router.navigateByUrl(path);
    }
  }

  icono(n: NotificacionDto): string {
    switch (n.severidad) {
      case 'ERROR':
        return 'error';
      case 'WARNING':
        return 'warning';
      default:
        return 'info';
    }
  }

  colorIcono(n: NotificacionDto): 'primary' | 'accent' | 'warn' | undefined {
    switch (n.severidad) {
      case 'ERROR':
        return 'warn';
      case 'WARNING':
        return 'accent';
      default:
        return 'primary';
    }
  }

  etiquetaTipo(t: string): string {
    switch (t) {
      case 'SUSCRIPCION':
        return 'Suscripción';
      case 'FACTURACION_CLIENTE':
        return 'Facturación clientes';
      default:
        return 'Sistema';
    }
  }
}
