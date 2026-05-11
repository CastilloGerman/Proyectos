import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';

interface BadgeConfig {
  cssClass: string;
  icon: string;
  tooltip?: string;
}

const BADGE_CONFIG: Record<string, BadgeConfig> = {
  'pagada':     { cssClass: 'badge-pagada',     icon: 'check_circle',    tooltip: 'Cobrada' },
  'no pagada':  { cssClass: 'badge-no-pagada',  icon: 'radio_button_unchecked', tooltip: 'Pendiente de cobro' },
  'parcial':    { cssClass: 'badge-parcial',     icon: 'timelapse',       tooltip: 'Cobro parcial' },
  'pendiente':  { cssClass: 'badge-pendiente',   icon: 'hourglass_empty', tooltip: 'Pendiente de respuesta' },
  'aceptado':   { cssClass: 'badge-aceptado',    icon: 'thumb_up',        tooltip: 'Presupuesto aceptado' },
  'rechazado':  { cssClass: 'badge-rechazado',   icon: 'thumb_down',      tooltip: 'Presupuesto rechazado' },
  'en ejecución': { cssClass: 'badge-ejecucion', icon: 'construction',  tooltip: 'Obra en curso' },
  'en ejecucion': { cssClass: 'badge-ejecucion', icon: 'construction',  tooltip: 'Obra en curso' },
};

@Component({
    selector: 'app-estado-badge',
    imports: [CommonModule, MatIconModule, MatTooltipModule, MatMenuModule],
    template: `
    @if (hasMenu) {
      <button
        type="button"
        class="badge-menu-trigger"
        [matMenuTriggerFor]="menu"
        [disabled]="menuDisabled"
        [matTooltip]="tooltipText"
      >
        <span class="estado-badge" [class]="config.cssClass">
          <mat-icon class="badge-icon">{{ config.icon }}</mat-icon>
          {{ estado }}
          <mat-icon class="caret">expand_more</mat-icon>
        </span>
      </button>
      <mat-menu #menu="matMenu" class="estado-badge-menu">
        @for (opt of menuOptions; track opt) {
          <button mat-menu-item type="button" (click)="onSelect(opt)" class="estado-badge-menu-item">
            {{ opt }}
          </button>
        }
      </mat-menu>
    } @else {
      <span
        class="estado-badge"
        [class]="config.cssClass"
        [matTooltip]="config.tooltip ?? estado"
      >
        <mat-icon class="badge-icon">{{ config.icon }}</mat-icon>
        {{ estado }}
      </span>
    }
  `,
    styles: [`
    .estado-badge {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      padding: 4px 10px 4px 8px;
      border-radius: 9999px;
      font-size: 0.75rem;
      font-weight: 500;
      white-space: nowrap;
      border: 1px solid transparent;
    }

    .badge-icon {
      font-size: 14px;
      width: 14px;
      height: 14px;
    }

    .caret {
      font-size: 14px;
      width: 14px;
      height: 14px;
      margin-left: 2px;
      opacity: 0.7;
    }

    .badge-menu-trigger {
      cursor: pointer;
      border: none;
      padding: 0;
      margin: 0;
      font: inherit;
      background: transparent;
      border-radius: 9999px;
      line-height: normal;
      vertical-align: middle;
    }

    .badge-menu-trigger:disabled {
      cursor: default;
      opacity: 0.65;
    }

    .badge-menu-trigger:focus-visible .estado-badge {
      outline: 2px solid rgba(59, 130, 246, 0.6);
      outline-offset: 2px;
    }

    .badge-pagada    { background: #dcfce7; color: #166534; }
    .badge-no-pagada { background: #ffedd5; color: #c2410c; }
    .badge-parcial   { background: #fef9c3; color: #a16207; }
    .badge-pendiente { background: #e0e7ff; color: #3730a3; }
    .badge-aceptado  { background: #dcfce7; color: #166534; }
    .badge-rechazado { background: #fee2e2; color: #b91c1c; }
    .badge-ejecucion { background: #ffedd5; color: #c2410c; }
  `]
})
export class EstadoBadgeComponent {
  @Input({ required: true }) estado!: string;
  /** Opciones para elegir estado (excluye el actual); si hay entradas, el badge abre menú al pulsar */
  @Input() menuOptions: string[] = [];
  @Input() menuDisabled = false;

  @Output() estadoSeleccionado = new EventEmitter<string>();

  get hasMenu(): boolean {
    return (this.menuOptions?.length ?? 0) > 0;
  }

  get tooltipText(): string {
    const base = this.config.tooltip ?? this.estado;
    return `${base} — Pulsa para cambiar`;
  }

  get config(): BadgeConfig {
    const key = (this.estado ?? '').toLowerCase();
    return BADGE_CONFIG[key] ?? { cssClass: 'badge-pendiente', icon: 'help_outline' };
  }

  onSelect(val: string): void {
    this.estadoSeleccionado.emit(val);
  }
}
