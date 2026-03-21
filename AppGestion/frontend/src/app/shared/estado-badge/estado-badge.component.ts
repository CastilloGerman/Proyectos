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
  'en ejecución': { cssClass: 'badge-ejecucion', icon: 'construction',  tooltip: 'Obra en curso' },
  'en ejecucion': { cssClass: 'badge-ejecucion', icon: 'construction',  tooltip: 'Obra en curso' },
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

    .badge-pagada    { background: #dcfce7; color: #166534; }
    .badge-no-pagada { background: #ffedd5; color: #c2410c; }
    .badge-parcial   { background: #fef9c3; color: #a16207; }
    .badge-pendiente { background: #e0e7ff; color: #3730a3; }
    .badge-aceptado  { background: #dcfce7; color: #166534; }
    .badge-rechazado { background: #fee2e2; color: #b91c1c; }
    .badge-ejecucion { background: #ffedd5; color: #c2410c; }
  `],
})
export class EstadoBadgeComponent {
  @Input({ required: true }) estado!: string;

  get config(): BadgeConfig {
    const key = (this.estado ?? '').toLowerCase();
    return BADGE_CONFIG[key] ?? { cssClass: 'badge-pendiente', icon: 'help_outline' };
  }
}
