import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

type BadgeVariant = 'pagada' | 'no-pagada' | 'parcial' | 'pendiente' | 'aceptado' | 'rechazado' | string;

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
};

@Component({
  selector: 'app-estado-badge',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatTooltipModule],
  template: `
    <span
      class="estado-badge"
      [class]="config.cssClass"
      [matTooltip]="config.tooltip ?? estado"
    >
      <mat-icon class="badge-icon">{{ config.icon }}</mat-icon>
      {{ estado }}
    </span>
  `,
  styles: [`
    .estado-badge {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      padding: 3px 10px 3px 6px;
      border-radius: 14px;
      font-size: 0.78rem;
      font-weight: 500;
      white-space: nowrap;
    }

    .badge-icon {
      font-size: 14px;
      width: 14px;
      height: 14px;
    }

    .badge-pagada    { background: #e8f5e9; color: #2e7d32; }
    .badge-no-pagada { background: #fff3e0; color: #e65100; }
    .badge-parcial   { background: #fff8e1; color: #f9a825; }
    .badge-pendiente { background: #f3f4fd; color: #3f51b5; }
    .badge-aceptado  { background: #e8f5e9; color: #2e7d32; }
    .badge-rechazado { background: #ffebee; color: #c62828; }
  `],
})
export class EstadoBadgeComponent {
  @Input({ required: true }) estado!: string;

  get config(): BadgeConfig {
    const key = (this.estado ?? '').toLowerCase();
    return BADGE_CONFIG[key] ?? { cssClass: 'badge-pendiente', icon: 'help_outline' };
  }
}
