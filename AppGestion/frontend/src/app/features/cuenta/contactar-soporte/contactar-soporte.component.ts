import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { SupportApiService } from '../../../core/services/support-api.service';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'app-contactar-soporte',
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
    styleUrl: './contactar-soporte.component.scss'
})
export class ContactarSoporteComponent {
  private readonly fb = inject(FormBuilder);
  private readonly supportApi = inject(SupportApiService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly route = inject(ActivatedRoute);
  private readonly translate = inject(TranslateService);

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
    this.supportApi.contact(fd).subscribe({
        next: (res) => {
          this.sending.set(false);
          this.snackBar.open(
            res.message ?? this.translate.instant('snack.contactSupportSent'),
            this.translate.instant('common.close'),
            { duration: 6000 },
          );
          this.form.reset();
          this.clearFiles(inputReset);
        },
        error: (err) => {
          this.sending.set(false);
          const raw = err?.error?.message ?? err?.error?.detail;
          const msg =
            typeof raw === 'string' && raw.trim() !== ''
              ? raw.trim()
              : this.translate.instant('snack.contactSupportFail');
          this.snackBar.open(msg, this.translate.instant('common.close'), { duration: 8000 });
        },
      });
  }
}
