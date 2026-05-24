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
import { TranslateModule, TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'app-cliente-form',
    imports: [
        ReactiveFormsModule,
        RouterLink,
        MatCardModule,
        MatFormFieldModule,
        MatInputModule,
        MatButtonModule,
        MatSnackBarModule,
        TranslateModule,
    ],
    template: `
    <div class="cliente-form">
      <mat-card>
        <mat-card-header>
          <mat-card-title>{{ isEdit ? ('cliForm.edit' | translate) : ('cliForm.new' | translate) }}</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>{{ 'cliForm.name' | translate }}</mat-label>
              <input matInput formControlName="nombre" [placeholder]="'cliForm.namePh' | translate">
              <mat-error>{{ 'cliForm.nameRequired' | translate }}</mat-error>
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>{{ 'cliForm.email' | translate }}</mat-label>
              <input matInput formControlName="email" type="email" [placeholder]="'cliForm.emailPh' | translate">
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>{{ 'cliForm.phone' | translate }}</mat-label>
              <input matInput formControlName="telefono" [placeholder]="'cliForm.phonePh' | translate">
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>{{ 'cliForm.address' | translate }}</mat-label>
              <input matInput formControlName="direccion" [placeholder]="'cliForm.addressPh' | translate">
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>{{ 'cliForm.postal' | translate }}</mat-label>
              <input matInput formControlName="codigoPostal" [placeholder]="'cliForm.postalPh' | translate">
              <mat-error>{{ 'cliForm.postalRequired' | translate }}</mat-error>
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>{{ 'cliForm.province' | translate }}</mat-label>
              <input matInput formControlName="provincia" [placeholder]="'cliForm.provincePh' | translate">
              <mat-error>{{ 'cliForm.provinceRequired' | translate }}</mat-error>
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>{{ 'cliForm.country' | translate }}</mat-label>
              <input matInput formControlName="pais" [placeholder]="'cliForm.countryPh' | translate">
              <mat-error>{{ 'cliForm.countryRequired' | translate }}</mat-error>
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>{{ 'cliForm.taxId' | translate }}</mat-label>
              <input matInput formControlName="dni" [placeholder]="'cliForm.taxIdPh' | translate">
              <mat-error>{{ 'cliForm.taxIdInvalid' | translate }}</mat-error>
            </mat-form-field>
            <div class="actions">
              <button mat-button type="button" routerLink="/clientes">{{ 'common.cancel' | translate }}</button>
              <button mat-raised-button color="primary" type="submit" [disabled]="form.invalid">
                {{ isEdit ? ('common.save' | translate) : ('common.create' | translate) }}
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
  `]
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
    private snackBar: MatSnackBar,
    private translate: TranslateService
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
        this.snackBar.open(
          this.translate.instant(this.isEdit ? 'snack.clientSavedUpdate' : 'snack.clientSavedCreate'),
          this.translate.instant('common.close'),
          { duration: 3000 },
        );
        this.router.navigate(['/clientes']);
      },
      error: (err) => {
        const msgRaw = err.error?.error || err.error?.message || '';
        const msg =
          typeof msgRaw === 'string' && String(msgRaw).trim() !== ''
            ? String(msgRaw).trim()
            : this.translate.instant('snack.clientSaveFail');
        this.snackBar.open(msg, this.translate.instant('common.close'), { duration: 5000 });
      },
    });
  }
}
