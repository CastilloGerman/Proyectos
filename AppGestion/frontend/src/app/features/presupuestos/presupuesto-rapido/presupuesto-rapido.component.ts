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
import { MatRadioModule } from '@angular/material/radio';
import { FormsModule } from '@angular/forms';
import { PresupuestoService } from '../../../core/services/presupuesto.service';
import { ClienteService } from '../../../core/services/cliente.service';
import { MaterialService } from '../../../core/services/material.service';
import { AuthService } from '../../../core/auth/auth.service';
import { Cliente } from '../../../core/models/cliente.model';
import { Material } from '../../../core/models/material.model';
import { PresupuestoItemRequest } from '../../../core/models/presupuesto.model';

@Component({
    selector: 'app-presupuesto-rapido',
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
        MatRadioModule,
        FormsModule,
    ],
    template: `
    <div class="rapido-wrap">
      <mat-card>
        <mat-card-header>
          <mat-card-title>Rápido en obra</mat-card-title>
          <mat-card-subtitle
            >Cliente, líneas de material y/o tareas manuales y PDF en pocos segundos. Ideal desde el móvil.</mat-card-subtitle
          >
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="crearYpdf()">
            <div class="cliente-block">
              <span class="modo-label">Cliente</span>
              <mat-radio-group
                [(ngModel)]="clienteModo"
                [ngModelOptions]="{standalone: true}"
                (ngModelChange)="onClienteModoChange($event)"
                class="modo-radios"
              >
                <mat-radio-button value="existente">Existente</mat-radio-button>
                <mat-radio-button value="nuevo">Nuevo rápido (solo nombre)</mat-radio-button>
              </mat-radio-group>
              @if (clienteModo === 'existente') {
                <mat-form-field appearance="outline" class="full" subscriptSizing="fixed">
                  <mat-label>Cliente</mat-label>
                  <mat-select formControlName="clienteId" required>
                    <mat-select-trigger>
                      {{ nombreClienteSeleccionado() }}
                    </mat-select-trigger>
                    @for (c of clientes; track c.id) {
                      <mat-option [value]="c.id">
                        <span class="opt-line">
                          <span>{{ c.nombre }}</span>
                          @if (c.estadoCliente === 'PROVISIONAL') {
                            <span class="badge-fiscal">Sin datos fiscales</span>
                          }
                        </span>
                      </mat-option>
                    }
                  </mat-select>
                </mat-form-field>
              } @else {
                <div class="nuevo-rapido">
                  <mat-form-field appearance="outline" class="nombre-nuevo" subscriptSizing="fixed">
                    <mat-label>Nombre del cliente</mat-label>
                    <input matInput [(ngModel)]="nombreClienteNuevo" [ngModelOptions]="{standalone: true}" placeholder="Ej. Juan García" autocomplete="name" />
                  </mat-form-field>
                  <button
                    type="button"
                    mat-stroked-button
                    color="primary"
                    (click)="crearClienteRapido()"
                    [disabled]="!auth.canMutate() || !nombreClienteNuevo.trim()"
                  >
                    Crear y usar
                  </button>
                </div>
              }
            </div>

            <div class="section materiales-section">
              <h3 class="section-title">Materiales</h3>
              <p class="section-hint">Elige artículos del catálogo (mismo flujo que en presupuesto normal).</p>
              <div formArrayName="materialItems" class="lines-block">
                @for (line of materialItems.controls; track line; let i = $index) {
                  <div [formGroupName]="i" class="line-row">
                    <mat-form-field appearance="outline" class="fg-mat" subscriptSizing="fixed">
                      <mat-label>Material</mat-label>
                      <mat-select formControlName="materialId" (selectionChange)="onMaterial(i, $event.value)">
                        <mat-option [value]="null">Seleccionar…</mat-option>
                        @for (m of materiales; track m.id) {
                          <mat-option [value]="m.id">{{ m.nombre }}</mat-option>
                        }
                      </mat-select>
                    </mat-form-field>
                    <mat-form-field appearance="outline" class="fg-desc" subscriptSizing="fixed">
                      <mat-label>Descripción</mat-label>
                      <input matInput formControlName="descripcion" autocomplete="off" />
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
                        (click)="removeMaterialLine(i)"
                        [disabled]="totalLineCount() <= 1"
                        matTooltip="Quitar línea">
                        <mat-icon>delete_outline</mat-icon>
                      </button>
                    </div>
                  </div>
                }
              </div>
              <button mat-stroked-button type="button" class="add-line" (click)="addMaterialLine()">
                <mat-icon>add</mat-icon>
                Añadir material
              </button>
            </div>

            <div class="section tareas-section">
              <h3 class="section-title">Tareas manuales</h3>
              <p class="section-hint">Trabajos sin artículo de catálogo (igual que en el presupuesto completo).</p>
              <div formArrayName="manualItems" class="lines-block">
                @for (line of manualItems.controls; track line; let i = $index) {
                  <div [formGroupName]="i" class="line-row manual-row">
                    <mat-form-field appearance="outline" class="fg-desc-manual" subscriptSizing="fixed">
                      <mat-label>Descripción</mat-label>
                      <input matInput formControlName="tareaManual" placeholder="Trabajo o concepto" autocomplete="off" />
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
                        (click)="removeManualLine(i)"
                        [disabled]="totalLineCount() <= 1"
                        matTooltip="Quitar línea">
                        <mat-icon>delete_outline</mat-icon>
                      </button>
                    </div>
                  </div>
                }
              </div>
              <button mat-stroked-button type="button" class="add-line" (click)="addManualLine()">
                <mat-icon>add</mat-icon>
                Añadir tarea manual
              </button>
            </div>

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
              <a
                class="wa-link"
                [href]="ultimoClienteWa"
                target="_blank"
                rel="noopener"
                aria-label="Abrir WhatsApp al cliente con un texto sugerido"
              >
                <img src="assets/whatsapp-logo.png" alt="" class="wa-logo" width="22" height="22" />
                <span class="wa-link-text">WhatsApp al cliente</span>
              </a>
              <span class="wa-hint">
                En muchos móviles el PDF se ofrece al compartir con WhatsApp; si no, abre el PDF en la otra pestaña y usa este
                enlace (texto sugerido, sin adjunto automático).
              </span>
            </div>
          }
        </mat-card-content>
      </mat-card>
    </div>
  `,
    styles: [`
    .rapido-wrap { max-width: 720px; margin: 24px auto; padding: 0 16px; }
    .cliente-block { margin-bottom: 16px; }
    .modo-label { display: block; font-size: 12px; color: var(--app-text-secondary, #64748b); margin-bottom: 8px; }
    .modo-radios { display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 12px; }
    .opt-line { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
    .badge-fiscal {
      font-size: 11px;
      font-weight: 500;
      color: #64748b;
      background: #f1f5f9;
      padding: 2px 8px;
      border-radius: 6px;
    }
    .nuevo-rapido { display: flex; flex-wrap: wrap; gap: 12px; align-items: flex-start; }
    .nombre-nuevo { flex: 1; min-width: 200px; }
    .full { width: 100%; display: block; margin-bottom: 12px; }
    .section { margin-bottom: 20px; }
    .section-title {
      font-size: 1rem;
      font-weight: 600;
      margin: 0 0 4px;
      color: var(--app-text-primary, #0f172a);
    }
    .section-hint {
      font-size: 13px;
      color: var(--app-text-secondary, #64748b);
      margin: 0 0 10px;
      line-height: 1.4;
    }
    .materiales-section {
      padding: 12px 14px;
      border-radius: var(--app-radius-md, 12px);
      border: 1px solid rgba(30, 58, 138, 0.12);
      background: rgba(30, 58, 138, 0.03);
    }
    .tareas-section {
      padding: 12px 14px;
      border-radius: var(--app-radius-md, 12px);
      border: 1px solid rgba(180, 83, 9, 0.22);
      /* Mismo criterio que materiales-section: tinte sutil, sin fondo fijo claro (rompe modo oscuro) */
      background: rgba(180, 83, 9, 0.06);
    }
    :host-context(html.app-dark-theme) .tareas-section {
      border-color: rgba(251, 191, 36, 0.28);
      background: rgba(251, 191, 36, 0.1);
    }
    .lines-block { display: flex; flex-direction: column; gap: 12px; margin-bottom: 8px; }
    .line-row {
      display: grid;
      grid-template-columns: minmax(100px, 0.95fr) minmax(140px, 1.6fr) 76px 84px 44px;
      gap: 8px;
      align-items: start;
    }
    .manual-row {
      grid-template-columns: minmax(160px, 2fr) 76px 84px 44px;
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
    .add-line { margin-bottom: 8px; }
    .actions { display: flex; gap: 12px; flex-wrap: wrap; margin-top: 20px; align-items: center; }
    .wa-row { margin-top: 20px; display: flex; flex-wrap: wrap; align-items: center; gap: 12px; }
    .wa-hint { font-size: 12px; color: #64748b; }
    .wa-link {
      display: inline-flex;
      flex-direction: row;
      align-items: center;
      gap: 10px;
      padding: 10px 16px;
      border-radius: var(--app-radius-md, 12px);
      border: 1px solid var(--app-border, rgba(15, 23, 42, 0.12));
      background: var(--app-bg-card, #fff);
      color: var(--app-text-primary, #0f172a);
      text-decoration: none;
      font-size: 14px;
      font-weight: 500;
      line-height: 1.2;
      transition: background 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease;
      box-sizing: border-box;
    }
    .wa-link:hover {
      background: var(--app-bg-page, #f8fafc);
      border-color: rgba(15, 23, 42, 0.18);
    }
    .wa-link:focus-visible {
      outline: 2px solid #1e3a8a;
      outline-offset: 2px;
    }
    .wa-link-text {
      color: inherit;
      white-space: nowrap;
    }
    .wa-logo {
      width: 22px;
      height: 22px;
      object-fit: contain;
      flex-shrink: 0;
      display: block;
    }
    @media (max-width: 700px) {
      .line-row {
        grid-template-columns: 1fr 1fr;
        grid-template-areas:
          'mat mat'
          'desc desc'
          'qty pu'
          'del del';
      }
      .manual-row {
        grid-template-columns: 1fr 1fr;
        grid-template-areas:
          'desc desc'
          'qty pu'
          'del del';
      }
      .fg-mat { grid-area: mat; }
      .fg-desc { grid-area: desc; }
      .fg-desc-manual { grid-area: desc; }
      .fg-qty { grid-area: qty; }
      .fg-pu { grid-area: pu; }
      .line-actions { grid-area: del; justify-content: flex-end; min-height: unset; padding-top: 0; }
    }
  `]
})
export class PresupuestoRapidoComponent implements OnInit {
  form = this.fb.group({
    clienteId: [null as number | null, Validators.required],
    materialItems: this.fb.array([this.createMaterialLine()]),
    manualItems: this.fb.array([]),
    ivaHabilitado: [true],
  });

  clientes: Cliente[] = [];
  materiales: Material[] = [];
  loading = false;
  ultimoClienteWa: string | null = null;
  clienteModo: 'existente' | 'nuevo' = 'existente';
  nombreClienteNuevo = '';

  constructor(
    private fb: FormBuilder,
    public auth: AuthService,
    private presupuestoService: PresupuestoService,
    private clienteService: ClienteService,
    private materialService: MaterialService,
    private snackBar: MatSnackBar
  ) {}

  get materialItems(): FormArray {
    return this.form.get('materialItems') as FormArray;
  }

  get manualItems(): FormArray {
    return this.form.get('manualItems') as FormArray;
  }

  totalLineCount(): number {
    return this.materialItems.length + this.manualItems.length;
  }

  ngOnInit(): void {
    this.clienteService.getAll().subscribe((c) => (this.clientes = c));
    this.materialService.getAll().subscribe((m) => (this.materiales = m));
  }

  /** Texto del campo cerrado: solo nombre (el badge solo se muestra en la lista). */
  nombreClienteSeleccionado(): string {
    const id = this.form.get('clienteId')?.value as number | null | undefined;
    if (id == null) return '';
    return this.clientes.find((c) => c.id === id)?.nombre ?? '';
  }

  onClienteModoChange(m: 'existente' | 'nuevo'): void {
    if (m === 'nuevo') {
      this.form.patchValue({ clienteId: null });
    }
  }

  crearClienteRapido(): void {
    const n = this.nombreClienteNuevo.trim();
    if (!n) return;
    this.clienteService.createProvisional({ nombre: n }).subscribe({
      next: (c) => {
        this.clientes = [...this.clientes, c].sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'));
        this.form.patchValue({ clienteId: c.id });
        this.snackBar.open('Cliente creado. Elige material y genera el PDF.', 'Cerrar', { duration: 3500 });
        this.clienteModo = 'existente';
        this.nombreClienteNuevo = '';
      },
      error: (err) => {
        const msg = err.error?.message || err.error?.detail || 'Error al crear el cliente';
        this.snackBar.open(msg, 'Cerrar', { duration: 5000 });
      },
    });
  }

  private createMaterialLine(): FormGroup {
    return this.fb.group({
      materialId: [null as number | null],
      descripcion: [''],
      cantidad: [1, [Validators.required, Validators.min(0.001)]],
      precioUnitario: [0, [Validators.required, Validators.min(0)]],
    });
  }

  private createManualLine(): FormGroup {
    return this.fb.group({
      tareaManual: ['', Validators.required],
      cantidad: [1, [Validators.required, Validators.min(0.001)]],
      precioUnitario: [0, [Validators.required, Validators.min(0)]],
    });
  }

  addMaterialLine(): void {
    this.materialItems.push(this.createMaterialLine());
  }

  addManualLine(): void {
    this.manualItems.push(this.createManualLine());
  }

  removeMaterialLine(index: number): void {
    if (this.totalLineCount() <= 1) return;
    this.materialItems.removeAt(index);
  }

  removeManualLine(index: number): void {
    if (this.totalLineCount() <= 1) return;
    this.manualItems.removeAt(index);
  }

  /** Si el material ya está en otra línea, una sola fila con la suma de cantidades. */
  onMaterial(index: number, id: number | null): void {
    if (id == null) return;
    const mat = this.materiales.find((x) => x.id === id);
    if (!mat) return;

    const indices: number[] = [];
    for (let i = 0; i < this.materialItems.length; i++) {
      if (this.materialItems.at(i).get('materialId')?.value === id) {
        indices.push(i);
      }
    }
    if (indices.length <= 1) {
      this.materialItems.at(index).patchValue({
        descripcion: mat.nombre,
        precioUnitario: mat.precioUnitario,
      });
      return;
    }

    const keep = Math.min(...indices);
    let totalCantidad = 0;
    for (const i of indices) {
      totalCantidad += +(this.materialItems.at(i).get('cantidad')?.value ?? 0);
    }
    (this.materialItems.at(keep) as FormGroup).patchValue({
      materialId: id,
      descripcion: mat.nombre,
      precioUnitario: mat.precioUnitario,
      cantidad: totalCantidad,
    });
    for (const i of indices
      .filter((idx) => idx !== keep)
      .sort((a, b) => b - a)) {
      this.materialItems.removeAt(i);
    }
  }

  private buildItemsPayload(): PresupuestoItemRequest[] {
    const out: PresupuestoItemRequest[] = [];
    for (const ctrl of this.materialItems.controls) {
      const v = (ctrl as FormGroup).getRawValue() as {
        materialId: number | null;
        cantidad: number;
        precioUnitario: number;
      };
      if (v.materialId) {
        out.push({
          materialId: v.materialId,
          cantidad: +v.cantidad,
          precioUnitario: +v.precioUnitario,
          aplicaIva: true,
          visiblePdf: true,
        });
      }
    }
    for (const ctrl of this.manualItems.controls) {
      const v = (ctrl as FormGroup).getRawValue() as {
        tareaManual: string;
        cantidad: number;
        precioUnitario: number;
      };
      const desc = v.tareaManual?.trim() || '';
      if (desc.length > 0) {
        out.push({
          tareaManual: desc,
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
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const itemPayload = this.buildItemsPayload();
    if (itemPayload.length === 0) {
      this.snackBar.open(
        'Añade al menos una línea: elige un material o una tarea manual con descripción.',
        'Cerrar',
        { duration: 4500 }
      );
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
            void this.sharePdfOrOpenTab(blob, pres.id, cli).finally(() => {
              this.loading = false;
            });
          },
          error: () => {
            this.loading = false;
            this.snackBar.open('Presupuesto creado; error al generar PDF', 'Cerrar', { duration: 4000 });
          },
        });
        this.snackBar.open('Presupuesto creado', 'Cerrar', { duration: 2500 });
      },
      error: (err) => {
        this.loading = false;
        this.snackBar.open(err.error?.message || 'Error al crear', 'Cerrar', { duration: 4000 });
      },
    });
  }

  private waMessageText(cli: Cliente | undefined, presupuestoId: number): string {
    return `Hola${cli?.nombre ? ' ' + cli.nombre : ''}, te envío el presupuesto #${presupuestoId}. ¿Te encaja?`;
  }

  private openPdfBlobInTab(blob: Blob): void {
    const url = URL.createObjectURL(blob);
    window.open(url, '_blank');
    setTimeout(() => URL.revokeObjectURL(url), 120_000);
  }

  private async sharePdfOrOpenTab(blob: Blob, presupuestoId: number, cli: Cliente | undefined): Promise<void> {
    const text = this.waMessageText(cli, presupuestoId);
    const file = new File([blob], `Presupuesto-${presupuestoId}.pdf`, { type: 'application/pdf' });

    if (typeof navigator.share === 'function' && typeof navigator.canShare === 'function') {
      const data: ShareData = { files: [file], text };
      if (navigator.canShare(data)) {
        try {
          await navigator.share(data);
          this.snackBar.open(
            'Elige WhatsApp en el menú para enviar el PDF y el mensaje (el texto lo puedes editar en la app).',
            'Cerrar',
            { duration: 6000 },
          );
          return;
        } catch (e: unknown) {
          const name = e && typeof e === 'object' && 'name' in e ? (e as { name: string }).name : '';
          if (name === 'AbortError') {
            this.openPdfBlobInTab(blob);
            return;
          }
          this.snackBar.open('No se pudo compartir. Se abre el PDF en una pestaña nueva.', 'Cerrar', {
            duration: 4000,
          });
          this.openPdfBlobInTab(blob);
          return;
        }
      }
    }

    this.openPdfBlobInTab(blob);
    this.snackBar.open(
      'Se abrió el PDF. En este equipo no se puede adjuntar solo con un clic: usa el botón de WhatsApp para el texto y adjunta el PDF desde la pestaña del documento.',
      'Cerrar',
      { duration: 7000 },
    );
  }

  private buildWaLink(cli: Cliente | undefined, presupuestoId: number): string | null {
    if (!cli?.telefono?.trim()) return null;
    let d = cli.telefono.replace(/\D/g, '');
    if (d.startsWith('00')) d = d.slice(2);
    if (d.length === 9) d = '34' + d;
    if (d.length < 10) return null;
    const msg = encodeURIComponent(this.waMessageText(cli, presupuestoId));
    return `https://wa.me/${d}?text=${msg}`;
  }
}
