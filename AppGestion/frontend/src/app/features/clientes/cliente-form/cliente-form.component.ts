import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { nifValidator } from '../../../shared/validators/nif.validator';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ClienteService } from '../../../core/services/cliente.service';

@Component({
  selector: 'app-cliente-form',
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
    <div class="cliente-form">
      <mat-card>
        <mat-card-header>
          <mat-card-title>{{ isEdit ? 'Editar cliente' : 'Nuevo cliente' }}</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Nombre</mat-label>
              <input matInput formControlName="nombre" placeholder="Nombre del cliente">
              <mat-error>El nombre es obligatorio</mat-error>
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Email</mat-label>
              <input matInput formControlName="email" type="email" placeholder="email@ejemplo.com">
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Teléfono</mat-label>
              <input matInput formControlName="telefono" placeholder="+34 600 000 000">
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Dirección</mat-label>
              <input matInput formControlName="direccion" placeholder="Dirección">
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Código postal</mat-label>
              <input matInput formControlName="codigoPostal" placeholder="28001">
              <mat-error>Código postal obligatorio para facturación</mat-error>
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Provincia</mat-label>
              <input matInput formControlName="provincia" placeholder="Madrid">
              <mat-error>Provincia obligatoria para facturación</mat-error>
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>País</mat-label>
              <input matInput formControlName="pais" placeholder="España">
              <mat-error>País obligatorio para facturación</mat-error>
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>NIF/CIF</mat-label>
              <input matInput formControlName="dni" placeholder="12345678A o B12345678">
              <mat-error>NIF/CIF no válido</mat-error>
            </mat-form-field>
            <div class="actions">
              <button mat-button type="button" routerLink="/clientes">Cancelar</button>
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
export class ClienteFormComponent implements OnInit {
  form: FormGroup;
  isEdit = false;
  id?: number;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private clienteService: ClienteService,
    private snackBar: MatSnackBar
  ) {
    this.form = this.fb.group({
      nombre: ['', Validators.required],
      email: [''],
      telefono: [''],
      direccion: [''],
      codigoPostal: ['', Validators.required],
      provincia: ['', Validators.required],
      pais: ['España', Validators.required],
      dni: ['', [Validators.required, nifValidator()]],
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'nuevo') {
      this.isEdit = true;
      this.id = +id;
      this.clienteService.getById(this.id).subscribe({
        next: (c) => {
          this.form.patchValue({
            nombre: c.nombre,
            email: c.email || '',
            telefono: c.telefono || '',
            direccion: c.direccion || '',
            codigoPostal: c.codigoPostal || '',
            provincia: c.provincia || '',
            pais: c.pais || 'España',
            dni: c.dni || '',
          });
        },
        error: () => this.router.navigate(['/clientes']),
      });
    }
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    const payload = {
      nombre: this.form.value.nombre,
      email: this.form.value.email || undefined,
      telefono: this.form.value.telefono || undefined,
      direccion: this.form.value.direccion || undefined,
      codigoPostal: this.form.value.codigoPostal || undefined,
      provincia: this.form.value.provincia || undefined,
      pais: this.form.value.pais || undefined,
      dni: this.form.value.dni || undefined,
    };
    const req = this.isEdit && this.id
      ? this.clienteService.update(this.id, payload)
      : this.clienteService.create(payload);
    req.subscribe({
      next: () => {
        this.snackBar.open(this.isEdit ? 'Cliente actualizado' : 'Cliente creado', 'Cerrar', { duration: 3000 });
        this.router.navigate(['/clientes']);
      },
      error: (err) => {
        const msg = err.error?.error || err.error?.message || 'Error al guardar';
        this.snackBar.open(msg, 'Cerrar', { duration: 5000 });
      },
    });
  }
}
