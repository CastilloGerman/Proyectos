import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FormsModule } from '@angular/forms';
import { PresupuestoService } from '../../../core/services/presupuesto.service';
import { ClienteService } from '../../../core/services/cliente.service';
import { MaterialService } from '../../../core/services/material.service';
import { Cliente } from '../../../core/models/cliente.model';
import { Material } from '../../../core/models/material.model';
import { PresupuestoItemRequest } from '../../../core/models/presupuesto.model';

const IVA_RATE = 0.21;

@Component({
  selector: 'app-presupuesto-form',
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
    MatIconModule,
    MatCheckboxModule,
    MatSnackBarModule,
    MatChipsModule,
    MatTooltipModule,
    FormsModule,
  ],
  template: `
    <div class="presupuesto-form">
      <mat-card>
        <mat-card-header>
          <mat-card-title>{{ isEdit ? 'Editar presupuesto' : 'Nuevo presupuesto' }}</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <!-- 1. Cliente y Estado -->
            <div class="form-row">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Cliente</mat-label>
                <mat-select formControlName="clienteId" required>
                  @for (c of clientes; track c.id) {
                    <mat-option [value]="c.id">{{ c.nombre }}</mat-option>
                  }
                </mat-select>
                <mat-error>Selecciona un cliente</mat-error>
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Estado</mat-label>
                <mat-select formControlName="estado">
                  <mat-option value="Pendiente">Pendiente</mat-option>
                  <mat-option value="Aceptado">Aceptado</mat-option>
                  <mat-option value="Rechazado">Rechazado</mat-option>
                </mat-select>
              </mat-form-field>
              <mat-checkbox formControlName="ivaHabilitado">IVA incluido</mat-checkbox>
            </div>

            <!-- 2. Materiales (primero, con línea visible al abrir) -->
            <div class="section materiales-section">
              <h3>Materiales</h3>
              @if (topMateriales.length > 0) {
                <div class="top-materiales">
                  <span class="top-label">Más usados:</span>
                  @for (m of topMateriales; track m.id) {
                    <button type="button" mat-stroked-button class="chip-btn" (click)="addMaterialFromTop(m)">
                      {{ m.nombre }}
                    </button>
                  }
                </div>
              }
              <div formArrayName="materialItems">
                @for (item of materialItems.controls; track item; let i = $index) {
                  <div [formGroupName]="i" class="item-row material-row">
                    @if (showVisibilityColumn) {
                      <mat-checkbox formControlName="visiblePdf" matTooltip="Visible en el PDF" class="visibility-check"></mat-checkbox>
                    }
                    <mat-form-field appearance="outline" class="material-select">
                      <mat-label>Material</mat-label>
                      <mat-select formControlName="materialId" (selectionChange)="onMaterialSelect(i, $event.value)">
                        <mat-option [value]="null">Seleccionar...</mat-option>
                        @for (m of materiales; track m.id) {
                          <mat-option [value]="m.id">{{ m.nombre }} ({{ m.precioUnitario | number:'1.2-2' }} €)</mat-option>
                        }
                      </mat-select>
                    </mat-form-field>
                    <mat-form-field appearance="outline">
                      <mat-label>Descripción</mat-label>
                      <input matInput formControlName="tareaManual" placeholder="Se rellena al seleccionar material">
                    </mat-form-field>
                    <mat-form-field appearance="outline">
                      <mat-label>Cantidad</mat-label>
                      <input matInput type="number" formControlName="cantidad" min="0.001" step="0.01">
                    </mat-form-field>
                    <mat-form-field appearance="outline">
                      <mat-label>Precio unit.</mat-label>
                      <input matInput type="number" formControlName="precioUnitario" min="0" step="0.01">
                    </mat-form-field>
                    <mat-checkbox formControlName="aplicaIva">IVA</mat-checkbox>
                    <button type="button" mat-icon-button color="warn" (click)="removeMaterialItem(i)" matTooltip="Eliminar">
                      <mat-icon>delete</mat-icon>
                    </button>
                  </div>
                }
              </div>
              <button type="button" mat-stroked-button (click)="addMaterialItem()">
                <mat-icon>add</mat-icon>
                Añadir material
              </button>
            </div>

            <!-- 3. Tareas manuales -->
            <div class="section tareas-section">
              <h3>Tareas manuales</h3>
              <div formArrayName="manualItems">
                @for (item of manualItems.controls; track item; let i = $index) {
                  <div [formGroupName]="i" class="item-row manual-row">
                    @if (showVisibilityColumn) {
                      <mat-checkbox formControlName="visiblePdf" matTooltip="Visible en el PDF" class="visibility-check"></mat-checkbox>
                    }
                    <mat-form-field appearance="outline" class="desc-wide">
                      <mat-label>Descripción</mat-label>
                      <input matInput formControlName="tareaManual" placeholder="Descripción del trabajo específico">
                    </mat-form-field>
                    <mat-form-field appearance="outline">
                      <mat-label>Cantidad</mat-label>
                      <input matInput type="number" formControlName="cantidad" min="0.001" step="0.01">
                    </mat-form-field>
                    <mat-form-field appearance="outline">
                      <mat-label>Precio unit.</mat-label>
                      <input matInput type="number" formControlName="precioUnitario" min="0" step="0.01">
                    </mat-form-field>
                    <mat-checkbox formControlName="aplicaIva">IVA</mat-checkbox>
                    <button type="button" mat-icon-button color="warn" (click)="removeManualItem(i)" matTooltip="Eliminar">
                      <mat-icon>delete</mat-icon>
                    </button>
                  </div>
                }
              </div>
              <button type="button" mat-stroked-button (click)="addManualTaskItem()">
                <mat-icon>add</mat-icon>
                Añadir tarea manual
              </button>
              <mat-checkbox [(ngModel)]="showVisibilityColumn" [ngModelOptions]="{standalone: true}" class="visibility-toggle">
                Mostrar opciones de visibilidad
              </mat-checkbox>
            </div>

            <!-- 4. Descuentos -->
            <div class="section discount-section">
              <h3>Descuentos globales</h3>
              <div class="discount-fields">
                <mat-form-field appearance="outline">
                  <mat-label>Descuento %</mat-label>
                  <input matInput type="number" formControlName="descuentoGlobalPorcentaje" min="0" max="100" step="0.01">
                </mat-form-field>
                <mat-form-field appearance="outline">
                  <mat-label>Descuento (€)</mat-label>
                  <input matInput type="number" formControlName="descuentoGlobalFijo" min="0" step="0.01">
                </mat-form-field>
                <mat-checkbox formControlName="descuentoAntesIva">Aplicar descuento antes del IVA</mat-checkbox>
              </div>
            </div>

            <!-- 5. Resumen de costes -->
            <div class="section cost-summary">
              <h3>Resumen de costes</h3>
              <div class="summary-rows">
                <div class="summary-row">
                  <span>Subtotal ítems:</span>
                  <span>{{ costesResumen.subtotalItems | number:'1.2-2' }} €</span>
                </div>
                @if (costesResumen.descuentoPorcentaje > 0 || costesResumen.descuentoFijo > 0) {
                  <div class="summary-row discount">
                    <span>Descuento aplicado:</span>
                    <span>- {{ costesResumen.descuentoTotal | number:'1.2-2' }} €</span>
                  </div>
                }
                <div class="summary-row">
                  <span>Base IVA:</span>
                  <span>{{ costesResumen.baseIva | number:'1.2-2' }} €</span>
                </div>
                @if (form.get('ivaHabilitado')?.value) {
                  <div class="summary-row">
                    <span>IVA (21%):</span>
                    <span>{{ costesResumen.iva | number:'1.2-2' }} €</span>
                  </div>
                }
                <div class="summary-row total">
                  <span>Total:</span>
                  <span>{{ costesResumen.total | number:'1.2-2' }} €</span>
                </div>
              </div>
            </div>

            <div class="actions">
              <button mat-button type="button" routerLink="/presupuestos">Cancelar</button>
              <button mat-raised-button color="primary" type="submit" [disabled]="form.invalid || form.pending">
                {{ isEdit ? 'Guardar' : 'Crear' }}
              </button>
            </div>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .full-width { width: 100%; display: block; margin-bottom: 16px; }
    .form-row { display: flex; flex-wrap: wrap; gap: 16px; align-items: center; margin-bottom: 20px; }
    .form-row mat-form-field { flex: 1; min-width: 200px; }

    .section {
      margin: 24px 0;
      padding: 20px;
      background: #fafafa;
      border-radius: 8px;
      border: 1px solid #eee;
    }
    .section h3 { margin: 0 0 16px 0; font-size: 16px; }

    .materiales-section { background: #f0f7ff; border-color: #c5d9f0; }
    .tareas-section { background: #fff8f0; border-color: #f0d9c5; }
    .discount-section { background: #f5f5f5; border-color: #e0e0e0; }
    .cost-summary { background: #e8f5e9; border: 1px solid #c8e6c9; }

    .top-materiales { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-bottom: 12px; }
    .top-label { font-size: 12px; color: #666; }
    .chip-btn { font-size: 12px; }

    .item-row {
      display: flex; flex-wrap: wrap; gap: 16px; align-items: center;
      margin-bottom: 16px; padding: 16px; background: #fff; border-radius: 8px; border: 1px solid #e0e0e0;
    }
    .item-row mat-form-field { flex: 1; min-width: 120px; }
    .item-row .material-select { min-width: 200px; }
    .item-row .desc-wide { min-width: 250px; }
    .item-row .visibility-check { margin-right: 8px; }
    .item-row mat-checkbox { margin: 0 8px; }

    .section > button { margin-top: 8px; margin-right: 16px; }
    .visibility-toggle { margin-top: 12px; display: block; }

    .discount-fields { display: flex; flex-wrap: wrap; gap: 16px; align-items: center; }
    .discount-fields mat-form-field { max-width: 120px; }

    .summary-rows { display: flex; flex-direction: column; gap: 8px; }
    .summary-row { display: flex; justify-content: space-between; font-size: 14px; }
    .summary-row.discount { color: #2e7d32; }
    .summary-row.total { font-weight: 600; font-size: 18px; margin-top: 8px; padding-top: 8px; border-top: 1px solid #a5d6a7; }

    .actions { display: flex; gap: 16px; margin-top: 24px; }
  `],
})
export class PresupuestoFormComponent implements OnInit {
  form: FormGroup;
  clientes: Cliente[] = [];
  materiales: Material[] = [];
  topMateriales: Material[] = [];
  showVisibilityColumn = false;
  isEdit = false;
  id?: number;

  get materialItems(): FormArray {
    return this.form.get('materialItems') as FormArray;
  }

  get manualItems(): FormArray {
    return this.form.get('manualItems') as FormArray;
  }

  get costesResumen() {
    return this.calcularCostesResumen();
  }

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private presupuestoService: PresupuestoService,
    private clienteService: ClienteService,
    private materialService: MaterialService,
    private snackBar: MatSnackBar
  ) {
    this.form = this.fb.group({
      clienteId: [null, Validators.required],
      estado: ['Pendiente'],
      ivaHabilitado: [true],
      descuentoGlobalPorcentaje: [0],
      descuentoGlobalFijo: [0],
      descuentoAntesIva: [true],
      materialItems: this.fb.array([]),
      manualItems: this.fb.array([]),
    });
  }

  ngOnInit(): void {
    const showError = (msg: string) => this.snackBar.open(msg, 'Cerrar', { duration: 5000 });
    this.clienteService.getAll().subscribe({
      next: (data) => (this.clientes = data),
      error: () => showError('Error al cargar clientes. Verifica que la API esté en ejecución.'),
    });
    this.materialService.getAll().subscribe({
      next: (data) => (this.materiales = data),
      error: () => showError('Error al cargar materiales.'),
    });
    this.materialService.getTopUsados().subscribe({
      next: (data) => (this.topMateriales = data),
      error: () => {}, // Opcional: top materiales
    });
    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'nuevo') {
      this.isEdit = true;
      this.id = +id;
      this.presupuestoService.getById(this.id).subscribe({
        next: (p) => {
          this.form.patchValue({
            clienteId: p.clienteId,
            estado: p.estado,
            ivaHabilitado: p.ivaHabilitado,
            descuentoGlobalPorcentaje: p.descuentoGlobalPorcentaje ?? 0,
            descuentoGlobalFijo: p.descuentoGlobalFijo ?? 0,
            descuentoAntesIva: p.descuentoAntesIva ?? true,
          });
          this.materialItems.clear();
          this.manualItems.clear();
          p.items.forEach((it) => {
            const group = this.createItemGroup({
              materialId: it.materialId ?? null,
              tareaManual: it.descripcion || '',
              cantidad: it.cantidad,
              precioUnitario: it.precioUnitario,
              aplicaIva: true,
              descuentoPorcentaje: 0,
              descuentoFijo: 0,
              visiblePdf: it.visiblePdf ?? true,
              isManualTask: it.esTareaManual ?? false,
            });
            if (it.esTareaManual) {
              this.manualItems.push(group);
            } else {
              this.materialItems.push(group);
            }
          });
        },
        error: () => this.router.navigate(['/presupuestos']),
      });
    }
    // No añadir fila vacía por defecto: el usuario añade desde "Más usados" o "Añadir material"
  }

  private createItemGroup(values: {
    materialId: number | null;
    tareaManual: string;
    cantidad: number;
    precioUnitario: number;
    aplicaIva: boolean;
    descuentoPorcentaje: number;
    descuentoFijo: number;
    visiblePdf: boolean;
    isManualTask: boolean;
  }): FormGroup {
    return this.fb.group({
      materialId: [values.materialId],
      tareaManual: [values.tareaManual, Validators.required],
      cantidad: [values.cantidad, [Validators.required, Validators.min(0.001)]],
      precioUnitario: [values.precioUnitario, [Validators.required, Validators.min(0)]],
      aplicaIva: [values.aplicaIva],
      descuentoPorcentaje: [values.descuentoPorcentaje],
      descuentoFijo: [values.descuentoFijo],
      visiblePdf: [values.visiblePdf],
      isManualTask: [values.isManualTask],
    });
  }

  addMaterialItem(): void {
    this.materialItems.push(
      this.createItemGroup({
        materialId: null,
        tareaManual: '',
        cantidad: 1,
        precioUnitario: 0,
        aplicaIva: true,
        descuentoPorcentaje: 0,
        descuentoFijo: 0,
        visiblePdf: true,
        isManualTask: false,
      })
    );
  }

  addManualTaskItem(): void {
    this.manualItems.push(
      this.createItemGroup({
        materialId: null,
        tareaManual: '',
        cantidad: 1,
        precioUnitario: 0,
        aplicaIva: true,
        descuentoPorcentaje: 0,
        descuentoFijo: 0,
        visiblePdf: true,
        isManualTask: true,
      })
    );
  }

  removeMaterialItem(index: number): void {
    this.materialItems.removeAt(index);
  }

  removeManualItem(index: number): void {
    this.manualItems.removeAt(index);
  }

  addMaterialFromTop(material: Material): void {
    this.materialItems.push(
      this.createItemGroup({
        materialId: material.id,
        tareaManual: material.nombre,
        cantidad: 1,
        precioUnitario: material.precioUnitario,
        aplicaIva: true,
        descuentoPorcentaje: 0,
        descuentoFijo: 0,
        visiblePdf: true,
        isManualTask: false,
      })
    );
    const idx = this.materialItems.length - 1;
    this.onMaterialSelect(idx, material.id);
  }

  onMaterialSelect(index: number, materialId: number | null): void {
    if (materialId == null) return;
    const material = this.materiales.find((m) => m.id === materialId);
    if (material) {
      const item = this.materialItems.at(index);
      item.patchValue({
        tareaManual: material.nombre,
        precioUnitario: material.precioUnitario,
      });
    }
  }

  private getAllItems(): { ctrl: any; isManual: boolean }[] {
    const result: { ctrl: any; isManual: boolean }[] = [];
    this.materialItems.controls.forEach((c) => result.push({ ctrl: c, isManual: false }));
    this.manualItems.controls.forEach((c) => result.push({ ctrl: c, isManual: true }));
    return result;
  }

  private calcularCostesResumen(): {
    subtotalItems: number;
    descuentoPorcentaje: number;
    descuentoFijo: number;
    descuentoTotal: number;
    baseIva: number;
    iva: number;
    total: number;
  } {
    const allItems = this.getAllItems();
    let subtotalItems = 0;
    let baseIva = 0;
    for (const { ctrl } of allItems) {
      const v = ctrl.value;
      const cantidad = +(v.cantidad ?? 0);
      const precio = +(v.precioUnitario ?? 0);
      const descPct = +(v.descuentoPorcentaje ?? 0);
      const descFijo = +(v.descuentoFijo ?? 0);
      let itemSub = cantidad * precio;
      itemSub = itemSub * (1 - descPct / 100) - descFijo;
      itemSub = Math.max(0, itemSub);
      subtotalItems += itemSub;
      if (v.aplicaIva) baseIva += itemSub;
    }
    const descPct = +(this.form.get('descuentoGlobalPorcentaje')?.value ?? 0);
    const descFijo = +(this.form.get('descuentoGlobalFijo')?.value ?? 0);
    const descuentoAntesIva = this.form.get('descuentoAntesIva')?.value !== false;
    let subtotal = subtotalItems;
    if (descuentoAntesIva) {
      subtotal = subtotal * (1 - descPct / 100) - descFijo;
      baseIva = baseIva * (1 - descPct / 100) - descFijo;
    } else {
      subtotal = subtotal * (1 - descPct / 100) - descFijo;
    }
    subtotal = Math.max(0, subtotal);
    baseIva = Math.max(0, baseIva);
    const descuentoTotal = subtotalItems - subtotal;
    const ivaHabilitado = this.form.get('ivaHabilitado')?.value !== false;
    const iva = ivaHabilitado ? baseIva * IVA_RATE : 0;
    const total = subtotal + iva;
    return {
      subtotalItems,
      descuentoPorcentaje: descPct,
      descuentoFijo: descFijo,
      descuentoTotal,
      baseIva,
      iva,
      total,
    };
  }

  onSubmit(): void {
    const allItems = this.getAllItems();
    const validItems = allItems.filter(({ ctrl }) => {
      const v = ctrl.value;
      const hasMaterial = v.materialId != null;
      const hasDesc = v.tareaManual != null && String(v.tareaManual).trim().length > 0;
      return hasMaterial || hasDesc;
    });
    if (this.form.invalid || validItems.length === 0) {
      this.form.markAllAsTouched();
      this.snackBar.open('Selecciona un cliente y añade al menos un material (selecciona del desplegable) o tarea manual', 'Cerrar', { duration: 4000 });
      return;
    }
    const items: PresupuestoItemRequest[] = validItems.map(({ ctrl }) => {
      const it = ctrl.value;
      return {
        materialId: it.materialId || undefined,
        tareaManual: it.tareaManual?.trim() || undefined,
        cantidad: +it.cantidad,
        precioUnitario: +it.precioUnitario,
        aplicaIva: it.aplicaIva,
        descuentoPorcentaje: it.descuentoPorcentaje ?? 0,
        descuentoFijo: it.descuentoFijo ?? 0,
        visiblePdf: it.visiblePdf ?? true,
      };
    });
    const value = this.form.value;
    const payload = {
      clienteId: value.clienteId,
      items,
      ivaHabilitado: value.ivaHabilitado,
      estado: value.estado,
      descuentoGlobalPorcentaje: +(value.descuentoGlobalPorcentaje ?? 0),
      descuentoGlobalFijo: +(value.descuentoGlobalFijo ?? 0),
      descuentoAntesIva: value.descuentoAntesIva !== false,
    };
    const req = this.isEdit && this.id
      ? this.presupuestoService.update(this.id, payload)
      : this.presupuestoService.create(payload);
    req.subscribe({
      next: (presupuesto) => {
        this.snackBar.open(this.isEdit ? 'Presupuesto actualizado' : 'Presupuesto creado', 'Cerrar', { duration: 3000 });
        this.presupuestoService.downloadPdf(presupuesto.id).subscribe({
          next: (blob) => {
            const url = URL.createObjectURL(blob);
            window.open(url, '_blank');
            URL.revokeObjectURL(url);
          },
          error: () => {},
        });
        this.router.navigate(['/presupuestos']);
      },
      error: (err) => {
        const msg = err.error?.message || err.error?.error || err.statusText || 'Error al guardar';
        this.snackBar.open(msg, 'Cerrar', { duration: 5000 });
      },
    });
  }
}
