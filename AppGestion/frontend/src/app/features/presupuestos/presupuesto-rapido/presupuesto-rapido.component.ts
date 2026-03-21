import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormArray,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { PresupuestoService } from '../../../core/services/presupuesto.service';
import { ClienteService } from '../../../core/services/cliente.service';
import { MaterialService } from '../../../core/services/material.service';
import { Cliente } from '../../../core/models/cliente.model';
import { Material } from '../../../core/models/material.model';
import { PresupuestoItemRequest } from '../../../core/models/presupuesto.model';

@Component({
  selector: 'app-presupuesto-rapido',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatCheckboxModule,
    MatSnackBarModule,
    MatIconModule,
    MatTooltipModule,
  ],
  template: `
    <div class="rapido-wrap">
      <mat-card>
        <mat-card-header>
          <mat-card-title>Rápido en obra</mat-card-title>
          <mat-card-subtitle
            >Cliente, una o varias líneas de material/servicio y PDF en pocos segundos. Ideal desde el móvil.</mat-card-subtitle
          >
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="crearYpdf()">
            <mat-form-field appearance="outline" class="full" subscriptSizing="fixed">
              <mat-label>Cliente</mat-label>
              <mat-select formControlName="clienteId" required>
                @for (c of clientes; track c.id) {
                  <mat-option [value]="c.id">{{ c.nombre }}</mat-option>
                }
              </mat-select>
            </mat-form-field>

            <p class="lines-label">Líneas del presupuesto</p>
            <div formArrayName="items" class="lines-block">
              @for (line of items.controls; track line; let i = $index) {
                <div [formGroupName]="i" class="line-row">
                  <mat-form-field appearance="outline" class="fg-mat" subscriptSizing="fixed">
                    <mat-label>Material</mat-label>
                    <mat-select formControlName="materialId" (selectionChange)="onMaterial(i, $event.value)">
                      <mat-option [value]="null">— Manual</mat-option>
                      @for (m of materiales; track m.id) {
                        <mat-option [value]="m.id">{{ m.nombre }}</mat-option>
                      }
                    </mat-select>
                  </mat-form-field>
                  <mat-form-field appearance="outline" class="fg-desc" subscriptSizing="fixed">
                    <mat-label>Descripción</mat-label>
                    <input matInput formControlName="descripcion" required autocomplete="off" />
                  </mat-form-field>
                  <mat-form-field appearance="outline" class="fg-qty" subscriptSizing="fixed">
                    <mat-label>Cant.</mat-label>
                    <input matInput type="number" formControlName="cantidad" min="0.001" step="0.01" />
                  </mat-form-field>
                  <mat-form-field appearance="outline" class="fg-pu" subscriptSizing="fixed">
                    <mat-label>P. unit.</mat-label>
                    <input matInput type="number" formControlName="precioUnitario" min="0" step="0.01" />
                  </mat-form-field>
                  <div class="line-actions">
                    <button
                      mat-icon-button
                      type="button"
                      class="btn-remove"
                      (click)="removeLine(i)"
                      [disabled]="items.length <= 1"
                      matTooltip="Quitar línea">
                      <mat-icon>delete_outline</mat-icon>
                    </button>
                  </div>
                </div>
              }
            </div>
            <button mat-stroked-button type="button" class="add-line" (click)="addLine()">
              <mat-icon>add</mat-icon>
              Añadir línea
            </button>

            <mat-checkbox formControlName="ivaHabilitado">IVA 21%</mat-checkbox>
            <div class="actions">
              <button mat-button type="button" routerLink="/presupuestos">Cancelar</button>
              <button mat-raised-button color="primary" type="submit" [disabled]="form.invalid || loading">
                <mat-icon>picture_as_pdf</mat-icon>
                Crear y abrir PDF
              </button>
            </div>
          </form>
          @if (ultimoClienteWa) {
            <div class="wa-row">
              <a mat-stroked-button color="accent" [href]="ultimoClienteWa" target="_blank" rel="noopener">
                <mat-icon>chat</mat-icon>
                WhatsApp al cliente
              </a>
              <span class="wa-hint">Abre la app con un texto sugerido (editable).</span>
            </div>
          }
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .rapido-wrap { max-width: 720px; margin: 24px auto; padding: 0 16px; }
    .full { width: 100%; display: block; margin-bottom: 12px; }
    .lines-label {
      font-size: 13px;
      font-weight: 600;
      color: rgba(0,0,0,0.7);
      margin: 8px 0 10px;
    }
    .lines-block { display: flex; flex-direction: column; gap: 12px; margin-bottom: 8px; }
    .line-row {
      display: grid;
      grid-template-columns: minmax(100px, 0.95fr) minmax(140px, 1.6fr) 76px 84px 44px;
      gap: 8px;
      align-items: start;
    }
    .line-row mat-form-field { margin: 0; width: 100%; }
    .line-actions {
      display: flex;
      align-items: center;
      justify-content: center;
      padding-top: 8px;
      min-height: 56px;
    }
    .btn-remove { margin-top: 0; }
    .add-line { margin-bottom: 16px; }
    .actions { display: flex; gap: 12px; flex-wrap: wrap; margin-top: 20px; align-items: center; }
    .wa-row { margin-top: 20px; display: flex; flex-wrap: wrap; align-items: center; gap: 12px; }
    .wa-hint { font-size: 12px; color: #64748b; }
    @media (max-width: 700px) {
      .line-row {
        grid-template-columns: 1fr 1fr;
        grid-template-areas:
          'mat mat'
          'desc desc'
          'qty pu'
          'del del';
      }
      .fg-mat { grid-area: mat; }
      .fg-desc { grid-area: desc; }
      .fg-qty { grid-area: qty; }
      .fg-pu { grid-area: pu; }
      .line-actions { grid-area: del; justify-content: flex-end; min-height: unset; padding-top: 0; }
    }
  `],
})
export class PresupuestoRapidoComponent implements OnInit {
  form = this.fb.group({
    clienteId: [null as number | null, Validators.required],
    items: this.fb.array([this.createItemLine()]),
    ivaHabilitado: [true],
  });

  clientes: Cliente[] = [];
  materiales: Material[] = [];
  loading = false;
  ultimoClienteWa: string | null = null;

  constructor(
    private fb: FormBuilder,
    private presupuestoService: PresupuestoService,
    private clienteService: ClienteService,
    private materialService: MaterialService,
    private snackBar: MatSnackBar
  ) {}

  get items(): FormArray {
    return this.form.get('items') as FormArray;
  }

  ngOnInit(): void {
    this.clienteService.getAll().subscribe((c) => (this.clientes = c));
    this.materialService.getAll().subscribe((m) => (this.materiales = m));
  }

  private createItemLine(): FormGroup {
    return this.fb.group({
      materialId: [null as number | null],
      descripcion: ['', Validators.required],
      cantidad: [1, [Validators.required, Validators.min(0.001)]],
      precioUnitario: [0, [Validators.required, Validators.min(0)]],
    });
  }

  addLine(): void {
    this.items.push(this.createItemLine());
  }

  removeLine(index: number): void {
    if (this.items.length <= 1) return;
    this.items.removeAt(index);
  }

  onMaterial(index: number, id: number | null): void {
    if (id == null) return;
    const mat = this.materiales.find((x) => x.id === id);
    if (mat) {
      this.items.at(index).patchValue({
        descripcion: mat.nombre,
        precioUnitario: mat.precioUnitario,
      });
    }
  }

  private buildItemsPayload(): PresupuestoItemRequest[] {
    const out: PresupuestoItemRequest[] = [];
    for (const ctrl of this.items.controls) {
      const v = (ctrl as FormGroup).getRawValue() as {
        materialId: number | null;
        descripcion: string;
        cantidad: number;
        precioUnitario: number;
      };
      const desc = v.descripcion?.trim() || '';
      if (v.materialId) {
        out.push({
          materialId: v.materialId,
          cantidad: +v.cantidad,
          precioUnitario: +v.precioUnitario,
          aplicaIva: true,
          visiblePdf: true,
        });
      } else {
        out.push({
          tareaManual: desc || 'Concepto',
          cantidad: +v.cantidad,
          precioUnitario: +v.precioUnitario,
          aplicaIva: true,
          visiblePdf: true,
        });
      }
    }
    return out;
  }

  crearYpdf(): void {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    const itemPayload = this.buildItemsPayload();
    if (itemPayload.length === 0) {
      this.snackBar.open('Añade al menos una línea válida', 'Cerrar', { duration: 3000 });
      return;
    }
    this.loading = true;
    this.ultimoClienteWa = null;
    const payload = {
      clienteId: v.clienteId!,
      estado: 'Pendiente',
      ivaHabilitado: v.ivaHabilitado !== false,
      items: itemPayload,
    };
    this.presupuestoService.create(payload).subscribe({
      next: (pres) => {
        const cli = this.clientes.find((c) => c.id === v.clienteId);
        this.ultimoClienteWa = this.buildWaLink(cli, pres.id);
        this.presupuestoService.downloadPdf(pres.id).subscribe({
          next: (blob) => {
            const url = URL.createObjectURL(blob);
            window.open(url, '_blank');
            URL.revokeObjectURL(url);
          },
          error: () => this.snackBar.open('Presupuesto creado; error al generar PDF', 'Cerrar', { duration: 4000 }),
        });
        this.snackBar.open('Presupuesto creado', 'Cerrar', { duration: 2500 });
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.snackBar.open(err.error?.message || 'Error al crear', 'Cerrar', { duration: 4000 });
      },
    });
  }

  private buildWaLink(cli: Cliente | undefined, presupuestoId: number): string | null {
    if (!cli?.telefono?.trim()) return null;
    let d = cli.telefono.replace(/\D/g, '');
    if (d.startsWith('00')) d = d.slice(2);
    if (d.length === 9) d = '34' + d;
    if (d.length < 10) return null;
    const msg = encodeURIComponent(
      `Hola${cli.nombre ? ' ' + cli.nombre : ''}, te envío el presupuesto #${presupuestoId}. ¿Te encaja?`
    );
    return `https://wa.me/${d}?text=${msg}`;
  }
}
