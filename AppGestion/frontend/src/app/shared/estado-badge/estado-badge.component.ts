import {
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  inject,
  SimpleChanges,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

type EstadoTipoId =
  | 'payPaid'
  | 'payUnpaid'
  | 'payPartial'
  | 'budPending'
  | 'budAccepted'
  | 'budRejected'
  | 'budInProgress';

interface BadgeStyle {
  cssClass: string;
  icon: string;
}

/** Claves visuales indexadas por estado normalizado (sin acentos, minúsculas). */
const ESTADO_VISUAL: Record<string, BadgeStyle> = {
  pagada: { cssClass: 'badge-pagada', icon: 'check_circle' },
  'no pagada': { cssClass: 'badge-no-pagada', icon: 'radio_button_unchecked' },
  parcial: { cssClass: 'badge-parcial', icon: 'timelapse' },
  pendiente: { cssClass: 'badge-pendiente', icon: 'hourglass_empty' },
  aceptado: { cssClass: 'badge-aceptado', icon: 'thumb_up' },
  rechazado: { cssClass: 'badge-rechazado', icon: 'thumb_down' },
  'en ejecución': { cssClass: 'badge-ejecucion', icon: 'construction' },
  'en ejecucion': { cssClass: 'badge-ejecucion', icon: 'construction' },
};

/** Misma normalización que en el dashboard para “En ejecución”. */
function normalizeEstadoKey(raw: string): string {
  return (raw ?? '')
    .trim()
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '');
}

const ESTADO_LABEL_ID: Record<string, EstadoTipoId> = {
  pagada: 'payPaid',
  'no pagada': 'payUnpaid',
  parcial: 'payPartial',
  pendiente: 'budPending',
  aceptado: 'budAccepted',
  rechazado: 'budRejected',
  'en ejecución': 'budInProgress',
  'en ejecucion': 'budInProgress',
};

@Component({
  selector: 'app-estado-badge',
  imports: [CommonModule, MatIconModule, MatTooltipModule, MatMenuModule, TranslateModule],
  template: `
    @if (hasMenu) {
      <button
        type="button"
        class="badge-menu-trigger"
        [matMenuTriggerFor]="menu"
        [disabled]="menuDisabled"
        [matTooltip]="tooltipTrigger()"
      >
        <span class="estado-badge" [class]="visual.cssClass">
          <mat-icon class="badge-icon">{{ visual.icon }}</mat-icon>
          {{ label(estado) }}
          <mat-icon class="caret">expand_more</mat-icon>
        </span>
      </button>
      <mat-menu #menu="matMenu" class="estado-badge-menu">
        @for (opt of menuOptions; track opt) {
          <button mat-menu-item type="button" (click)="onSelect(opt)" class="estado-badge-menu-item">
            {{ label(opt) }}
          </button>
        }
      </mat-menu>
    } @else {
      <span class="estado-badge" [class]="visual.cssClass" [matTooltip]="tooltipReadonly(estado)">
        <mat-icon class="badge-icon">{{ visual.icon }}</mat-icon>
        {{ label(estado) }}
      </span>
    }
  `,
  styles: `
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

    .badge-pagada {
      background: #dcfce7;
      color: #166534;
    }
    .badge-no-pagada {
      background: #ffedd5;
      color: #c2410c;
    }
    .badge-parcial {
      background: #fef9c3;
      color: #a16207;
    }
    .badge-pendiente {
      background: #e0e7ff;
      color: #3730a3;
    }
    .badge-aceptado {
      background: #dcfce7;
      color: #166534;
    }
    .badge-rechazado {
      background: #fee2e2;
      color: #b91c1c;
    }
    .badge-ejecucion {
      background: #ffedd5;
      color: #c2410c;
    }
  `,
})
export class EstadoBadgeComponent implements OnChanges {
  private readonly translate = inject(TranslateService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    this.translate.onLangChange.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.cdr.markForCheck();
    });
  }

  @Input({ required: true }) estado!: string;
  @Input() menuOptions: string[] = [];
  @Input() menuDisabled = false;

  @Output() estadoSeleccionado = new EventEmitter<string>();

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['estado']) {
      this.cdr.markForCheck();
    }
  }

  get hasMenu(): boolean {
    return (this.menuOptions?.length ?? 0) > 0;
  }

  get visual(): BadgeStyle {
    const key = normalizeEstadoKey(this.estado ?? '');
    return ESTADO_VISUAL[key] ?? { cssClass: 'badge-pendiente', icon: 'help_outline' };
  }

  label(raw: string): string {
    const norm = normalizeEstadoKey(raw ?? '');
    const id = ESTADO_LABEL_ID[norm];
    if (id) {
      return this.translate.instant(`est.lbl.${id}`);
    }
    return (raw ?? '').trim() || '';
  }

  tooltipReadonly(raw: string): string {
    const norm = normalizeEstadoKey(raw ?? '');
    const id = ESTADO_LABEL_ID[norm];
    if (id) {
      return this.translate.instant(`est.tip.${id}`);
    }
    return this.label(raw);
  }

  tooltipTrigger(): string {
    const tip = this.tooltipReadonly(this.estado);
    if (!this.hasMenu) return tip;
    return `${tip}\n${this.translate.instant('est.pressToChange')}`;
  }

  onSelect(val: string): void {
    this.estadoSeleccionado.emit(val);
  }
}
