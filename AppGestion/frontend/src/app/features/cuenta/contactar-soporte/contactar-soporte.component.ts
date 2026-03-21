import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-contactar-soporte',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  templateUrl: './contactar-soporte.component.html',
  styleUrl: './contactar-soporte.component.scss',
})
export class ContactarSoporteComponent {
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly snackBar = inject(MatSnackBar);
  private readonly route = inject(ActivatedRoute);

  readonly sending = signal(false);
  readonly selectedFiles = signal<File[]>([]);

  readonly form = this.fb.nonNullable.group({
    asunto: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(200)]],
    mensaje: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(8000)]],
  });

  constructor() {
    const motivo = this.route.snapshot.queryParamMap.get('motivo');
    if (motivo === 'reporte') {
      this.form.patchValue({ asunto: 'Reporte / incidencia en la aplicación' });
    }
  }

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const list = input.files ? Array.from(input.files) : [];
    this.selectedFiles.set(list);
  }

  clearFiles(input: HTMLInputElement): void {
    input.value = '';
    this.selectedFiles.set([]);
  }

  enviar(inputReset: HTMLInputElement): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { asunto, mensaje } = this.form.getRawValue();
    const fd = new FormData();
    fd.append('asunto', asunto);
    fd.append('mensaje', mensaje);
    for (const f of this.selectedFiles()) {
      fd.append('archivos', f, f.name);
    }

    this.sending.set(true);
    this.http
      .post<{ message: string }>(`${environment.apiUrl}/auth/support/contact`, fd)
      .subscribe({
        next: (res) => {
          this.sending.set(false);
          this.snackBar.open(res.message ?? 'Mensaje enviado', 'Cerrar', { duration: 6000 });
          this.form.reset();
          this.clearFiles(inputReset);
        },
        error: (err) => {
          this.sending.set(false);
          const msg =
            err?.error?.message ??
            err?.error?.detail ??
            'No se pudo enviar el mensaje. Revisa la conexión o la configuración de correo.';
          this.snackBar.open(msg, 'Cerrar', { duration: 8000 });
        },
      });
  }
}
