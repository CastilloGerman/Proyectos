import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MaterialService } from '../../../core/services/material.service';

@Component({
  selector: 'app-material-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSnackBarModule,
  ],
  template: `
    <div class="material-form">
      <mat-card>
        <mat-card-header>
          <mat-card-title>{{ isEdit ? 'Editar material' : 'Nuevo material' }}</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Nombre</mat-label>
              <input matInput formControlName="nombre" placeholder="Nombre del material">
              <mat-error>El nombre es obligatorio</mat-error>
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Precio unitario</mat-label>
              <input matInput formControlName="precioUnitario" type="number" min="0" step="0.01" placeholder="0.00">
              <mat-error>El precio es obligatorio y debe ser mayor o igual a 0</mat-error>
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Unidad de medida</mat-label>
              <input matInput formControlName="unidadMedida" placeholder="ud, kg, m, etc.">
            </mat-form-field>
            <div class="actions">
              <button mat-button type="button" routerLink="/materiales">Cancelar</button>
              <button mat-raised-button color="primary" type="submit" [disabled]="form.invalid">
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

    .actions {
      display: flex;
      gap: 16px;
      margin-top: 24px;
    }
  `],
})
export class MaterialFormComponent implements OnInit {
  form: FormGroup;
  isEdit = false;
  id?: number;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private materialService: MaterialService,
    private snackBar: MatSnackBar
  ) {
    this.form = this.fb.group({
      nombre: ['', Validators.required],
      precioUnitario: [0, [Validators.required, Validators.min(0)]],
      unidadMedida: ['ud'],
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'nuevo') {
      this.isEdit = true;
      this.id = +id;
      this.materialService.getById(this.id).subscribe({
        next: (m) => {
          this.form.patchValue({
            nombre: m.nombre,
            precioUnitario: m.precioUnitario,
            unidadMedida: m.unidadMedida || 'ud',
          });
        },
        error: () => this.router.navigate(['/materiales']),
      });
    }
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    const payload = {
      nombre: this.form.value.nombre,
      precioUnitario: +this.form.value.precioUnitario,
      unidadMedida: this.form.value.unidadMedida?.trim() || 'ud',
    };
    const req = this.isEdit && this.id
      ? this.materialService.update(this.id, payload)
      : this.materialService.create(payload);
    req.subscribe({
      next: () => {
        this.snackBar.open(this.isEdit ? 'Material actualizado' : 'Material creado', 'Cerrar', { duration: 3000 });
        this.router.navigate(['/materiales']);
      },
      error: (err) => {
        const msg = err.error?.error || err.error?.message || 'Error al guardar';
        this.snackBar.open(msg, 'Cerrar', { duration: 5000 });
      },
    });
  }
}
