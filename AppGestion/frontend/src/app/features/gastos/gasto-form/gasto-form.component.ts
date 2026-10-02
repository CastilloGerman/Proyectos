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
import { MatIconModule } from '@angular/material/icon';
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
    MatIconModule,
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
          @if (!isEdit) {
            <section class="ai-import" aria-labelledby="ai-import-title">
              <h2 id="ai-import-title">{{ 'gastos.aiTitle' | translate }}</h2>
              <p>{{ 'gastos.aiDescription' | translate }}</p>
              <input
                #receiptInput
                class="file-input"
                type="file"
                accept="image/jpeg,image/png,image/webp,application/pdf,.jpg,.jpeg,.png,.webp,.pdf"
                (change)="onReceiptSelected($event)"
                [disabled]="isExtracting"
              />
              @if (selectedFile) {
                <p class="selected-file">{{ selectedFile.name }}</p>
              }
              <div class="ai-actions">
                <button mat-stroked-button type="button" (click)="receiptInput.click()" [disabled]="isExtracting">
                  <mat-icon>attach_file</mat-icon>
                  {{ 'gastos.aiChooseFile' | translate }}
                </button>
                <button mat-raised-button color="accent" type="button" (click)="extractDraft()"
                        [disabled]="!selectedFile || isExtracting">
                  <mat-icon>auto_awesome</mat-icon>
                  {{ (isExtracting ? 'gastos.aiWorking' : 'gastos.aiExtract') | translate }}
                </button>
              </div>
              @if (aiDocumentInvalid) {
                <p class="ai-message ai-warning" role="alert">{{ 'gastos.aiInvalidDocument' | translate }}</p>
              }
              @if (aiReviewFields.length > 0) {
                <div class="ai-message ai-warning" role="status">
                  <p>{{ 'gastos.aiReview' | translate }}</p>
                  <ul>
                    @for (field of aiReviewFields; track field) {
                      <li>{{ ('gastos.aiFields.' + field) | translate }}</li>
                    }
                  </ul>
                </div>
              }
              @if (aiExtracted && !aiDocumentInvalid) {
                <p class="ai-message ai-success" role="status">{{ 'gastos.aiDraftReady' | translate }}</p>
              }
            </section>
          }
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

    .ai-import {
      border-top: 1px solid rgba(0, 0, 0, 0.12);
      margin-top: 24px;
      padding-top: 20px;
    }

    .ai-import h2 {
      font-size: 1.1rem;
      margin: 0 0 8px;
    }

    .ai-import > p {
      color: rgba(0, 0, 0, 0.7);
    }

    .file-input {
      position: absolute;
      width: 1px;
      height: 1px;
      padding: 0;
      margin: -1px;
      overflow: hidden;
      clip: rect(0, 0, 0, 0);
      white-space: nowrap;
      border: 0;
    }

    .selected-file {
      font-size: 0.9rem;
      overflow-wrap: anywhere;
    }

    .ai-actions {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      margin-top: 12px;
    }

    .ai-message {
      margin: 16px 0 0;
      padding: 12px;
      border-radius: 8px;
    }

    .ai-message p { margin: 0; }
    .ai-message ul { margin-bottom: 0; }
    .ai-warning { background: #fff4e5; color: #663c00; }
    .ai-success { background: #e8f5e9; color: #1b5e20; }
  `],
})
export class GastoFormComponent implements OnInit {
  form: FormGroup;
  isEdit = false;
  id?: number;
  categorias = GASTO_CATEGORIAS;
  tiposIva = TIPOS_IVA;
  selectedFile: File | null = null;
  isExtracting = false;
  aiExtracted = false;
  aiDocumentInvalid = false;
  aiReviewFields: string[] = [];

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

  onReceiptSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
    this.aiExtracted = false;
    this.aiDocumentInvalid = false;
    this.aiReviewFields = [];
  }

  extractDraft(): void {
    if (!this.selectedFile || this.isExtracting) return;
    this.isExtracting = true;
    this.aiExtracted = false;
    this.aiDocumentInvalid = false;
    this.aiReviewFields = [];
    this.gastoService.extractDraft(this.selectedFile).subscribe({
      next: (draft) => {
        this.isExtracting = false;
        if (!draft.esDocumentoValido) {
          this.aiDocumentInvalid = true;
          return;
        }
        this.form.patchValue({
          proveedor: draft.proveedor ?? '',
          concepto: draft.concepto ?? '',
          fecha: draft.fecha ? new Date(`${draft.fecha}T12:00:00`) : null,
          baseImponible: draft.baseImponible,
          tipoIva: draft.tipoIva,
          categoria: draft.categoria,
        });
        this.aiReviewFields = draft.camposDudosos ?? [];
        this.aiExtracted = true;
      },
      error: () => {
        this.isExtracting = false;
        this.snackBar.open(
          this.translate.instant('gastos.aiExtractFail'),
          this.translate.instant('common.close'),
          { duration: 5000 },
        );
      },
    });
  }
}
