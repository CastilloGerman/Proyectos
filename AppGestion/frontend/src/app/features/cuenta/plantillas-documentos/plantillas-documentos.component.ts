import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTabsModule } from '@angular/material/tabs';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ConfigService } from '../../../core/services/config.service';
import { AuthService } from '../../../core/auth/auth.service';
import { Empresa } from '../../../core/models/empresa.model';

const MAX_PIE = 1000;

@Component({
  selector: 'app-plantillas-documentos',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatTabsModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  templateUrl: './plantillas-documentos.component.html',
  styleUrl: './plantillas-documentos.component.scss',
})
export class PlantillasDocumentosComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly config = inject(ConfigService);
  private readonly auth = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly loadError = signal(false);
  readonly maxPie = MAX_PIE;

  readonly form = this.fb.nonNullable.group({
    notasPiePresupuesto: ['', [Validators.maxLength(MAX_PIE)]],
    notasPieFactura: ['', [Validators.maxLength(MAX_PIE)]],
  });

  ngOnInit(): void {
    this.cargar();
  }

  get puedeEditar(): boolean {
    return this.auth.canMutate();
  }

  cargar(): void {
    this.loadError.set(false);
    this.loading.set(true);
    this.config.getEmpresa().subscribe({
      next: (e: Empresa) => {
        this.form.patchValue({
          notasPiePresupuesto: e.notasPiePresupuesto ?? '',
          notasPieFactura: e.notasPieFactura ?? '',
        });
        this.form.markAsPristine();
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set(true);
        this.loading.set(false);
        this.snackBar.open('No se pudieron cargar las plantillas', 'Cerrar', { duration: 4000 });
      },
    });
  }

  guardar(): void {
    if (!this.puedeEditar || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.saving.set(true);
    this.config
      .patchPlantillasPdf({
        notasPiePresupuesto: v.notasPiePresupuesto.trim() || null,
        notasPieFactura: v.notasPieFactura.trim() || null,
      })
      .subscribe({
        next: () => {
          this.form.markAsPristine();
          this.snackBar.open('Plantillas guardadas. Los nuevos PDF usarán estos textos.', 'Cerrar', {
            duration: 4500,
          });
          this.saving.set(false);
        },
        error: (err) => {
          this.saving.set(false);
          const msg =
            err?.error?.message ?? err?.error?.detail ?? 'No se pudo guardar. Revisa la conexión o tus permisos.';
          this.snackBar.open(msg, 'Cerrar', { duration: 5000 });
        },
      });
  }
}
