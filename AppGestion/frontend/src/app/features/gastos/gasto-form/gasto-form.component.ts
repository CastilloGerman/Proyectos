import { DecimalPipe } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { GastoService } from '../../../core/services/gasto.service';
import {
  calcularCuotaIva,
  GASTO_CATEGORIAS,
  GastoCategoria,
  TIPOS_IVA,
} from '../../../core/models/gasto.model';

@Component({
  selector: 'app-gasto-form',
  imports: [
    DecimalPipe,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatSnackBarModule,
    MatDatepickerModule,
    MatNativeDateModule,
    TranslateModule,
  ],
  template: `
    <div class="gasto-form">
      <mat-card>
        <mat-card-header>
          <mat-card-title>{{ (isEdit ? 'gastos.editTitle' : 'gastos.newTitle') | translate }}</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>{{ 'gastos.fieldSupplier' | translate }}</mat-label>
              <input matInput formControlName="proveedor" />
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>{{ 'gastos.fieldConcept' | translate }}</mat-label>
              <input matInput formControlName="concepto" />
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>{{ 'gastos.fieldDate' | translate }}</mat-label>
              <input matInput [matDatepicker]="picker" formControlName="fecha" />
              <mat-datepicker-toggle matIconSuffix [for]="picker"></mat-datepicker-toggle>
              <mat-datepicker #picker></mat-datepicker>
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>{{ 'gastos.fieldBase' | translate }}</mat-label>
              <input matInput formControlName="baseImponible" type="number" min="0" step="0.01" />
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>{{ 'gastos.fieldVatType' | translate }}</mat-label>
              <mat-select formControlName="tipoIva">
                @for (t of tiposIva; track t) {
                  <mat-option [value]="t">{{ t }} %</mat-option>
                }
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>{{ 'gastos.fieldCategory' | translate }}</mat-label>
              <mat-select formControlName="categoria">
                @for (c of categorias; track c) {
                  <mat-option [value]="c">{{ ('gastos.category.' + c) | translate }}</mat-option>
                }
              </mat-select>
            </mat-form-field>
            <p class="preview">
              {{ 'gastos.previewVat' | translate }}: {{ previewCuotaIva | number:'1.2-2' }} € ·
              {{ 'gastos.previewTotal' | translate }}: {{ previewTotal | number:'1.2-2' }} €
            </p>
            <div class="actions">
              <button mat-button type="button" routerLink="/gastos">{{ 'common.cancel' | translate }}</button>
              <button mat-raised-button color="primary" type="submit" [disabled]="form.invalid">
                {{ (isEdit ? 'common.save' : 'common.create') | translate }}
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

    .preview {
      margin: 8px 0 16px;
      color: rgba(0, 0, 0, 0.7);
    }

    .actions {
      display: flex;
      gap: 16px;
      margin-top: 24px;
    }
  `],
})
export class GastoFormComponent implements OnInit {
  form: FormGroup;
  isEdit = false;
  id?: number;
  categorias = GASTO_CATEGORIAS;
  tiposIva = TIPOS_IVA;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private gastoService: GastoService,
    private snackBar: MatSnackBar,
    private translate: TranslateService,
  ) {
    this.form = this.fb.group({
      proveedor: ['', Validators.required],
      concepto: ['', Validators.required],
      fecha: [new Date(), Validators.required],
      baseImponible: [0, [Validators.required, Validators.min(0)]],
      tipoIva: [21, Validators.required],
      categoria: ['OTROS' as GastoCategoria, Validators.required],
    });
  }

  get previewCuotaIva(): number {
    const base = Number(this.form.get('baseImponible')?.value ?? 0);
    const tipo = Number(this.form.get('tipoIva')?.value ?? 0);
    return calcularCuotaIva(base, tipo);
  }

  get previewTotal(): number {
    const base = Number(this.form.get('baseImponible')?.value ?? 0);
    return base + this.previewCuotaIva;
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'nuevo') {
      this.isEdit = true;
      this.id = +id;
      this.gastoService.getById(this.id).subscribe({
        next: (g) => {
          this.form.patchValue({
            proveedor: g.proveedor,
            concepto: g.concepto,
            fecha: new Date(g.fecha + 'T12:00:00'),
            baseImponible: g.baseImponible,
            tipoIva: g.tipoIva,
            categoria: g.categoria,
          });
        },
        error: () => {
          this.snackBar.open(
            this.translate.instant('gastos.loadFail'),
            this.translate.instant('common.close'),
            { duration: 3000 },
          );
          this.router.navigate(['/gastos']);
        },
      });
    }
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    const raw = this.form.getRawValue();
    const fecha = raw.fecha instanceof Date
      ? raw.fecha.toISOString().slice(0, 10)
      : String(raw.fecha).slice(0, 10);
    const payload = {
      proveedor: raw.proveedor,
      concepto: raw.concepto,
      fecha,
      baseImponible: Number(raw.baseImponible),
      tipoIva: Number(raw.tipoIva),
      categoria: raw.categoria as GastoCategoria,
    };
    const req$ = this.isEdit && this.id
      ? this.gastoService.update(this.id, payload)
      : this.gastoService.create(payload);
    req$.subscribe({
      next: () => {
        this.snackBar.open(
          this.translate.instant(this.isEdit ? 'gastos.saved' : 'gastos.created'),
          this.translate.instant('common.close'),
          { duration: 3000 },
        );
        this.router.navigate(['/gastos']);
      },
      error: () => {
        this.snackBar.open(
          this.translate.instant('gastos.saveFail'),
          this.translate.instant('common.close'),
          { duration: 3000 },
        );
      },
    });
  }
}
