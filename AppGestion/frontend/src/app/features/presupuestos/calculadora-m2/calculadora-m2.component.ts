import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

export interface CalculadoraResult {
  area: number;
  descripcion: string;
}

export interface Shape {
  id: string;
  label: string;
  icon: string;
  fields: { id: string; label: string }[];
  calc: (v: Record<string, number>) => number;
  formula: (v: Record<string, number>) => string;
}

@Component({
  selector: 'app-calculadora-m2',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatInputModule,
    MatFormFieldModule,
    MatIconModule,
    MatTooltipModule,
  ],
  template: `
    <div class="calc-wrapper">
      <h2 class="calc-title">Calculadora m²</h2>

      <div class="shape-grid">
        <button
          *ngFor="let s of shapes"
          class="shape-btn"
          [class.active]="currentShape.id === s.id"
          (click)="selectShape(s)"
          type="button"
        >
          <mat-icon>{{ s.icon }}</mat-icon>
          <span>{{ s.label }}</span>
        </button>
      </div>

      <div class="fields-area">
        <mat-form-field
          *ngFor="let f of currentShape.fields"
          appearance="outline"
          class="field-full"
        >
          <mat-label>{{ f.label }}</mat-label>
          <input
            matInput
            type="number"
            min="0"
            step="0.01"
            [(ngModel)]="values[f.id]"
            (ngModelChange)="calculate()"
            placeholder="0.00"
          />
          <span matTextSuffix>m</span>
        </mat-form-field>

        <mat-form-field appearance="outline" class="field-full">
          <mat-label>Descripción (opcional)</mat-label>
          <input matInput [(ngModel)]="descripcion" placeholder="ej: sala principal" />
        </mat-form-field>
      </div>

      <div class="result-card" *ngIf="result !== null">
        <div class="result-area-value">
          <span class="result-number">{{ result | number: '1.2-2' }}</span>
          <span class="result-unit">m²</span>
        </div>
        <div class="result-formula">{{ formulaDisplay }}</div>
      </div>

      <div class="result-card result-empty" *ngIf="result === null">
        <span>Introduce las medidas para calcular</span>
      </div>

      <div class="history-section" *ngIf="history.length > 0">
        <div class="history-header">
          <span class="history-title">Zonas añadidas</span>
          <button mat-button color="warn" (click)="clearHistory()" type="button">Borrar</button>
        </div>
        <div class="history-item" *ngFor="let h of history; let i = index">
          <div class="history-desc">
            <span>{{ h.descripcion }}</span>
            <span class="history-formula">{{ h.formula }}</span>
          </div>
          <div class="history-right">
            <span class="history-value">{{ h.area | number: '1.2-2' }} m²</span>
            <button mat-icon-button (click)="removeHistoryItem(i)" type="button" aria-label="Eliminar zona">
              <mat-icon>close</mat-icon>
            </button>
          </div>
        </div>
        <div class="history-total">
          <span>Total acumulado</span>
          <strong>{{ totalAcumulado | number: '1.2-2' }} m²</strong>
        </div>
      </div>

      <div class="actions">
        <button mat-stroked-button (click)="addToHistory()" [disabled]="result === null" type="button">
          <mat-icon>add</mat-icon> Añadir zona
        </button>
        <div class="actions-right">
          <button mat-stroked-button (click)="cancel()" type="button">Cancelar</button>
          <button
            mat-flat-button
            color="primary"
            (click)="insert()"
            [disabled]="result === null && history.length === 0"
            type="button"
          >
            Insertar {{ insertValue | number: '1.2-2' }} m²
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .calc-wrapper {
        padding: 8px 0;
        min-width: 380px;
        max-width: 480px;
      }

      .calc-title {
        font-size: 18px;
        font-weight: 500;
        margin: 0 0 16px 0;
        padding: 0 24px;
      }

      .shape-grid {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: 8px;
        padding: 0 24px 16px;
      }

      .shape-btn {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 4px;
        padding: 10px 6px;
        border: 1px solid #e0e0e0;
        border-radius: 8px;
        background: white;
        cursor: pointer;
        font-size: 12px;
        color: #555;
        transition: all 0.15s;
      }

      .shape-btn mat-icon {
        font-size: 20px;
        height: 20px;
        width: 20px;
      }

      .shape-btn.active {
        border-color: #1976d2;
        background: #e3f2fd;
        color: #1565c0;
      }

      .fields-area {
        padding: 0 24px;
        display: flex;
        flex-direction: column;
        gap: 4px;
      }

      .field-full {
        width: 100%;
      }

      .result-card {
        margin: 8px 24px 12px;
        padding: 14px 16px;
        background: #f5f5f5;
        border-radius: 8px;
      }

      .result-empty {
        font-size: 13px;
        color: #888;
        text-align: center;
      }

      .result-area-value {
        display: flex;
        align-items: baseline;
        gap: 6px;
      }

      .result-number {
        font-size: 28px;
        font-weight: 500;
      }

      .result-unit {
        font-size: 16px;
        color: #666;
      }

      .result-formula {
        font-size: 12px;
        color: #888;
        margin-top: 2px;
      }

      .history-section {
        margin: 0 24px 12px;
        border: 1px solid #e0e0e0;
        border-radius: 8px;
        overflow: hidden;
      }

      .history-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 8px 12px 4px;
      }

      .history-title {
        font-size: 13px;
        font-weight: 500;
        color: #555;
      }

      .history-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 4px 12px 4px 16px;
        border-top: 1px solid #f0f0f0;
        font-size: 13px;
      }

      .history-desc {
        display: flex;
        flex-direction: column;
        gap: 1px;
      }

      .history-formula {
        font-size: 11px;
        color: #aaa;
      }

      .history-right {
        display: flex;
        align-items: center;
        gap: 4px;
      }

      .history-value {
        font-weight: 500;
      }

      .history-total {
        display: flex;
        justify-content: space-between;
        padding: 10px 16px;
        background: #f5f5f5;
        font-size: 13px;
        border-top: 1px solid #e0e0e0;
      }

      .actions {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 8px 24px 0;
        gap: 8px;
      }

      .actions-right {
        display: flex;
        gap: 8px;
      }
    `,
  ],
})
export class CalculadoraM2Component {
  shapes: Shape[] = [
    {
      id: 'rect',
      label: 'Rectángulo',
      icon: 'crop_square',
      fields: [
        { id: 'a', label: 'Largo (m)' },
        { id: 'b', label: 'Ancho (m)' },
      ],
      calc: (v) => v['a'] * v['b'],
      formula: (v) => `${v['a']} × ${v['b']}`,
    },
    {
      id: 'tri',
      label: 'Triángulo',
      icon: 'change_history',
      fields: [
        { id: 'a', label: 'Base (m)' },
        { id: 'b', label: 'Altura (m)' },
      ],
      calc: (v) => (v['a'] * v['b']) / 2,
      formula: (v) => `(${v['a']} × ${v['b']}) / 2`,
    },
    {
      id: 'circ',
      label: 'Círculo',
      icon: 'radio_button_unchecked',
      fields: [{ id: 'a', label: 'Radio (m)' }],
      calc: (v) => Math.PI * v['a'] * v['a'],
      formula: (v) => `π × ${v['a']}²`,
    },
    {
      id: 'trap',
      label: 'Trapecio',
      icon: 'pentagon',
      fields: [
        { id: 'a', label: 'Base mayor (m)' },
        { id: 'b', label: 'Base menor (m)' },
        { id: 'c', label: 'Altura (m)' },
      ],
      calc: (v) => ((v['a'] + v['b']) / 2) * v['c'],
      formula: (v) => `((${v['a']}+${v['b']}) / 2) × ${v['c']}`,
    },
  ];

  currentShape: Shape = this.shapes[0];
  values: Record<string, number> = {};
  descripcion = '';
  result: number | null = null;
  formulaDisplay = '';

  history: { area: number; descripcion: string; formula: string }[] = [];

  get totalAcumulado(): number {
    return this.history.reduce((s, h) => s + h.area, 0);
  }

  get insertValue(): number {
    if (this.history.length > 0) return this.totalAcumulado;
    return this.result ?? 0;
  }

  constructor(
    public dialogRef: MatDialogRef<CalculadoraM2Component>,
    @Inject(MAT_DIALOG_DATA) public data: Record<string, never>,
  ) {}

  selectShape(s: Shape): void {
    this.currentShape = s;
    this.values = {};
    this.result = null;
    this.formulaDisplay = '';
  }

  calculate(): void {
    const allFilled = this.currentShape.fields.every(
      (f) => this.values[f.id] !== undefined && this.values[f.id] > 0,
    );
    if (!allFilled) {
      this.result = null;
      return;
    }
    this.result = this.currentShape.calc(this.values);
    this.formulaDisplay = this.currentShape.formula(this.values);
  }

  addToHistory(): void {
    if (this.result === null) return;
    this.history.push({
      area: this.result,
      descripcion: this.descripcion || this.currentShape.label,
      formula: this.formulaDisplay,
    });
    this.values = {};
    this.result = null;
    this.descripcion = '';
    this.formulaDisplay = '';
  }

  removeHistoryItem(i: number): void {
    this.history.splice(i, 1);
  }

  clearHistory(): void {
    this.history = [];
  }

  insert(): void {
    const area = this.history.length > 0 ? this.totalAcumulado : this.result!;
    const desc =
      this.history.length > 1
        ? `Total ${this.history.length} zonas`
        : (this.history[0]?.descripcion ?? this.descripcion ?? '');
    this.dialogRef.close({ area: parseFloat(area.toFixed(4)), descripcion: desc } as CalculadoraResult);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }
}
