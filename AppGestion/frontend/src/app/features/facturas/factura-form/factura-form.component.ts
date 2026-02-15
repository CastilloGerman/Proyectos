import { Component, OnInit } from '@angular/core';
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
import { FacturaService } from '../../../core/services/factura.service';
import { ClienteService } from '../../../core/services/cliente.service';
import { PresupuestoService } from '../../../core/services/presupuesto.service';
import { Cliente } from '../../../core/models/cliente.model';
import { Presupuesto } from '../../../core/models/presupuesto.model';
import { FacturaItemRequest } from '../../../core/models/factura.model';

@Component({
  selector: 'app-factura-form',
  standalone: true,
  imports: [
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
  ],
  template: `
    <div class="factura-form">
      <mat-card>
        <mat-card-header>
          <mat-card-title>{{ isEdit ? 'Editar factura' : 'Nueva factura' }}</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
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
              <mat-label>Presupuesto (opcional)</mat-label>
              <mat-select formControlName="presupuestoId">
                <mat-option [value]="null">Ninguno</mat-option>
                @for (p of presupuestos; track p.id) {
                  <mat-option [value]="p.id">{{ p.clienteNombre }} - {{ p.total | number:'1.2-2' }} €</mat-option>
                }
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Nº Factura</mat-label>
              <input matInput formControlName="numeroFactura" placeholder="Se genera automático si se deja vacío">
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Fecha vencimiento</mat-label>
              <input matInput formControlName="fechaVencimiento" type="date">
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Método de pago</mat-label>
              <mat-select formControlName="metodoPago">
                <mat-option value="Transferencia">Transferencia</mat-option>
                <mat-option value="Efectivo">Efectivo</mat-option>
                <mat-option value="Tarjeta">Tarjeta</mat-option>
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Estado de pago</mat-label>
              <mat-select formControlName="estadoPago">
                <mat-option value="No Pagada">No Pagada</mat-option>
                <mat-option value="Pagada">Pagada</mat-option>
                <mat-option value="Parcial">Parcial</mat-option>
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Notas</mat-label>
              <textarea matInput formControlName="notas" rows="2"></textarea>
            </mat-form-field>
            <mat-checkbox formControlName="ivaHabilitado">IVA incluido</mat-checkbox>
            <div formArrayName="items" class="items-section">
              <div class="items-header">
                <h3>Líneas</h3>
                <button type="button" mat-stroked-button (click)="addItem()">
                  <mat-icon>add</mat-icon>
                  Añadir línea
                </button>
              </div>
              @for (item of items.controls; track $index; let i = $index) {
                <div [formGroupName]="i" class="item-row">
                  <mat-form-field appearance="outline">
                    <mat-label>Descripción</mat-label>
                    <input matInput formControlName="tareaManual" placeholder="Descripción">
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
                  <button type="button" mat-icon-button color="warn" (click)="removeItem(i)">
                    <mat-icon>delete</mat-icon>
                  </button>
                </div>
              }
            </div>
            <div class="actions">
              <button mat-button type="button" routerLink="/facturas">Cancelar</button>
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
    .full-width {
      width: 100%;
      display: block;
      margin-bottom: 16px;
    }

    .items-section {
      margin: 24px 0;
    }

    .items-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }

    .item-row {
      display: flex;
      flex-wrap: wrap;
      gap: 16px;
      align-items: center;
      margin-bottom: 16px;
      padding: 16px;
      background: #f5f5f5;
      border-radius: 8px;
    }

    .item-row mat-form-field {
      flex: 1;
      min-width: 150px;
    }

    .item-row mat-checkbox {
      margin: 0 8px;
    }

    .actions {
      display: flex;
      gap: 16px;
      margin-top: 24px;
    }
  `],
})
export class FacturaFormComponent implements OnInit {
  form: FormGroup;
  clientes: Cliente[] = [];
  presupuestos: Presupuesto[] = [];
  isEdit = false;
  id?: number;

  get items(): FormArray {
    return this.form.get('items') as FormArray;
  }

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private facturaService: FacturaService,
    private clienteService: ClienteService,
    private presupuestoService: PresupuestoService,
    private snackBar: MatSnackBar
  ) {
    this.form = this.fb.group({
      clienteId: [null, Validators.required],
      presupuestoId: [null],
      numeroFactura: [''],
      fechaVencimiento: [''],
      metodoPago: ['Transferencia'],
      estadoPago: ['No Pagada'],
      notas: [''],
      ivaHabilitado: [true],
      items: this.fb.array([], Validators.required),
    });
  }

  ngOnInit(): void {
    this.clienteService.getAll().subscribe((data) => (this.clientes = data));
    this.presupuestoService.getAll().subscribe((data) => (this.presupuestos = data));
    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'nuevo') {
      this.isEdit = true;
      this.id = +id;
      this.facturaService.getById(this.id).subscribe({
        next: (f) => {
          this.form.patchValue({
            clienteId: f.clienteId,
            presupuestoId: f.presupuestoId || null,
            numeroFactura: f.numeroFactura,
            fechaVencimiento: f.fechaVencimiento || '',
            metodoPago: f.metodoPago,
            estadoPago: f.estadoPago,
            notas: f.notas || '',
            ivaHabilitado: f.ivaHabilitado,
          });
          this.items.clear();
          f.items.forEach((it) =>
            this.items.push(
              this.fb.group({
                materialId: [null],
                tareaManual: [it.descripcion || ''],
                cantidad: [it.cantidad, [Validators.required, Validators.min(0.001)]],
                precioUnitario: [it.precioUnitario, [Validators.required, Validators.min(0)]],
                aplicaIva: [true],
              })
            )
          );
        },
        error: () => this.router.navigate(['/facturas']),
      });
    } else {
      this.addItem();
    }
  }

  addItem(): void {
    this.items.push(
      this.fb.group({
        materialId: [null],
        tareaManual: ['', Validators.required],
        cantidad: [1, [Validators.required, Validators.min(0.001)]],
        precioUnitario: [0, [Validators.required, Validators.min(0)]],
        aplicaIva: [true],
      })
    );
  }

  removeItem(index: number): void {
    this.items.removeAt(index);
  }

  onSubmit(): void {
    if (this.form.invalid || this.items.length === 0) {
      this.form.markAllAsTouched();
      this.snackBar.open('Completa todos los campos y añade al menos una línea', 'Cerrar', { duration: 4000 });
      return;
    }
    const value = this.form.value;
    const items: FacturaItemRequest[] = value.items.map((it: any) => ({
      materialId: it.materialId || undefined,
      tareaManual: it.tareaManual || undefined,
      cantidad: +it.cantidad,
      precioUnitario: +it.precioUnitario,
      aplicaIva: it.aplicaIva,
    }));
    const payload: any = {
      clienteId: value.clienteId,
      items,
      metodoPago: value.metodoPago,
      estadoPago: value.estadoPago,
      notas: value.notas || undefined,
      ivaHabilitado: value.ivaHabilitado,
    };
    if (value.presupuestoId) payload.presupuestoId = value.presupuestoId;
    if (value.numeroFactura?.trim()) payload.numeroFactura = value.numeroFactura.trim();
    if (value.fechaVencimiento) payload.fechaVencimiento = value.fechaVencimiento;
    const req = this.isEdit && this.id
      ? this.facturaService.update(this.id, payload)
      : this.facturaService.create(payload);
    req.subscribe({
      next: () => {
        this.snackBar.open(this.isEdit ? 'Factura actualizada' : 'Factura creada', 'Cerrar', { duration: 3000 });
        this.router.navigate(['/facturas']);
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Error al guardar', 'Cerrar', { duration: 4000 });
      },
    });
  }
}
