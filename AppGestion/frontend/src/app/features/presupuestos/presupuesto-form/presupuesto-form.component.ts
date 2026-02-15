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
import { PresupuestoService } from '../../../core/services/presupuesto.service';
import { ClienteService } from '../../../core/services/cliente.service';
import { Cliente } from '../../../core/models/cliente.model';
import { PresupuestoItemRequest } from '../../../core/models/presupuesto.model';

@Component({
  selector: 'app-presupuesto-form',
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
    <div class="presupuesto-form">
      <mat-card>
        <mat-card-header>
          <mat-card-title>{{ isEdit ? 'Editar presupuesto' : 'Nuevo presupuesto' }}</mat-card-title>
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
              <mat-label>Estado</mat-label>
              <mat-select formControlName="estado">
                <mat-option value="Pendiente">Pendiente</mat-option>
                <mat-option value="Aceptado">Aceptado</mat-option>
                <mat-option value="Rechazado">Rechazado</mat-option>
              </mat-select>
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
                    <input matInput formControlName="tareaManual" placeholder="Descripción del trabajo">
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
export class PresupuestoFormComponent implements OnInit {
  form: FormGroup;
  clientes: Cliente[] = [];
  isEdit = false;
  id?: number;

  get items(): FormArray {
    return this.form.get('items') as FormArray;
  }

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private presupuestoService: PresupuestoService,
    private clienteService: ClienteService,
    private snackBar: MatSnackBar
  ) {
    this.form = this.fb.group({
      clienteId: [null, Validators.required],
      estado: ['Pendiente'],
      ivaHabilitado: [true],
      items: this.fb.array([], Validators.required),
    });
  }

  ngOnInit(): void {
    this.clienteService.getAll().subscribe((data) => (this.clientes = data));
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
          });
          this.items.clear();
          p.items.forEach((it) =>
            this.items.push(
              this.fb.group({
                materialId: [it.id],
                tareaManual: [it.descripcion || ''],
                cantidad: [it.cantidad, [Validators.required, Validators.min(0.001)]],
                precioUnitario: [it.precioUnitario, [Validators.required, Validators.min(0)]],
                aplicaIva: [true],
                descuentoPorcentaje: [0],
                descuentoFijo: [0],
              })
            )
          );
        },
        error: () => this.router.navigate(['/presupuestos']),
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
        descuentoPorcentaje: [0],
        descuentoFijo: [0],
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
    const items: PresupuestoItemRequest[] = value.items.map((it: any) => ({
      materialId: it.materialId || undefined,
      tareaManual: it.tareaManual || undefined,
      cantidad: +it.cantidad,
      precioUnitario: +it.precioUnitario,
      aplicaIva: it.aplicaIva,
      descuentoPorcentaje: it.descuentoPorcentaje ?? 0,
      descuentoFijo: it.descuentoFijo ?? 0,
    }));
    const payload = {
      clienteId: value.clienteId,
      items,
      ivaHabilitado: value.ivaHabilitado,
      estado: value.estado,
    };
    const req = this.isEdit && this.id
      ? this.presupuestoService.update(this.id, payload)
      : this.presupuestoService.create(payload);
    req.subscribe({
      next: () => {
        this.snackBar.open(this.isEdit ? 'Presupuesto actualizado' : 'Presupuesto creado', 'Cerrar', { duration: 3000 });
        this.router.navigate(['/presupuestos']);
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Error al guardar', 'Cerrar', { duration: 4000 });
      },
    });
  }
}
