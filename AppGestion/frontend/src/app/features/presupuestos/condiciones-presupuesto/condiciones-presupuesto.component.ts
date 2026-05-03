import { Component, Input, forwardRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import {
  CondicionesPresupuestoFormValue,
  PresupuestoCondicionDisponible,
} from '../../../core/models/presupuesto-condiciones.model';

/**
 * Bloque de UI para condiciones predefinidas (catálogo desde API) + nota libre.
 * ControlValueAccessor: un único valor compuesto para integrarlo en ReactiveForms sin exponer claves al usuario.
 */
@Component({
  selector: 'app-condiciones-presupuesto',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatSlideToggleModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CondicionesPresupuestoComponent),
      multi: true,
    },
  ],
  template: `
    <div class="cond-wrap">
      <div class="cond-block">
        <h3 class="cond-title">Condiciones del presupuesto</h3>
        <p class="cond-sub">Marca las que quieras incluir al final del documento</p>
        @if (disponibles.length === 0) {
          <p class="cond-empty">Cargando condiciones…</p>
        } @else {
          @for (c of disponibles; track c.clave) {
            <div class="cond-row">
              <mat-slide-toggle
                class="cond-toggle"
                [checked]="isOn(c.clave)"
                (change)="onToggle(c.clave, $event.checked)"
                [disabled]="disabled"
              />
              <span class="cond-text">{{ c.textoVisible }}</span>
            </div>
          }
        }
      </div>
      @if (showNota) {
        <div class="nota-block">
          <h3 class="cond-title">Nota adicional (opcional)</h3>
          <mat-form-field appearance="outline" class="nota-field" subscriptSizing="fixed">
            <textarea
              matInput
              rows="4"
              [disabled]="disabled"
              [placeholder]="notaPlaceholder"
              [ngModel]="inner.notaAdicional"
              (ngModelChange)="onNota($event)"
              [ngModelOptions]="{ standalone: true }"
            ></textarea>
          </mat-form-field>
        </div>
      }
    </div>
  `,
  styles: `
    .cond-wrap {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }
    .cond-block,
    .nota-block {
      margin: 0;
    }
    .cond-title {
      margin: 0 0 0.35rem 0;
      font-size: 1rem;
      font-weight: 600;
      color: var(--app-text-primary, #0f172a);
    }
    .cond-sub {
      margin: 0 0 0.75rem 0;
      font-size: 0.8125rem;
      color: var(--app-text-secondary, #64748b);
      line-height: 1.4;
    }
    .cond-empty {
      margin: 0;
      font-size: 0.875rem;
      color: var(--app-text-muted, #94a3b8);
    }
    .cond-row {
      display: flex;
      align-items: flex-start;
      gap: 0.75rem;
      padding: 0.5rem 0;
      border-bottom: 1px solid var(--app-border, #f1f5f9);
    }
    .cond-row:last-child {
      border-bottom: none;
    }
    .cond-toggle {
      flex-shrink: 0;
      margin-top: 0.15rem;
    }
    /* Área táctil amplia en móvil */
    :host ::ng-deep .cond-toggle .mdc-switch {
      transform: scale(1.05);
    }
    .cond-text {
      flex: 1;
      font-size: 0.9rem;
      line-height: 1.45;
      color: var(--app-text-primary, #334155);
    }
    .nota-field {
      width: 100%;
    }
    .nota-block .cond-title {
      margin-bottom: 0.5rem;
    }
  `,
})
export class CondicionesPresupuestoComponent implements ControlValueAccessor {
  /** Lista desde GET condiciones-disponibles (orden servidor). */
  @Input({ required: true }) disponibles: PresupuestoCondicionDisponible[] = [];
  /** En perfil solo interesa el bloque de toggles, sin textarea. */
  @Input() showNota = true;

  readonly notaPlaceholder =
    'Ejemplo: "Presupuesto válido hasta el 15 de mayo" o cualquier acuerdo especial con el cliente.';

  inner: CondicionesPresupuestoFormValue = { condicionesActivas: [], notaAdicional: '' };
  disabled = false;

  private onChange: (v: CondicionesPresupuestoFormValue) => void = () => {};
  private onTouched: () => void = () => {};

  isOn(clave: string): boolean {
    return this.inner.condicionesActivas.includes(clave);
  }

  onToggle(clave: string, checked: boolean): void {
    const set = new Set(this.inner.condicionesActivas);
    if (checked) {
      set.add(clave);
    } else {
      set.delete(clave);
    }
    this.inner = {
      ...this.inner,
      condicionesActivas: Array.from(set),
    };
    this.onChange(this.inner);
    this.onTouched();
  }

  onNota(text: string): void {
    this.inner = { ...this.inner, notaAdicional: text };
    this.onChange(this.inner);
    this.onTouched();
  }

  writeValue(obj: CondicionesPresupuestoFormValue | null): void {
    if (obj == null) {
      this.inner = { condicionesActivas: [], notaAdicional: '' };
      return;
    }
    this.inner = {
      condicionesActivas: [...(obj.condicionesActivas ?? [])],
      notaAdicional: obj.notaAdicional ?? '',
    };
  }

  registerOnChange(fn: (v: CondicionesPresupuestoFormValue) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }
}
