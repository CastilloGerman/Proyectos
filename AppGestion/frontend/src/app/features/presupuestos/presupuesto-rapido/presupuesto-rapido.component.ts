import { CommonModule } from '@angular/common';
import { Component, DestroyRef, ElementRef, OnInit, ViewChild, effect, inject } from '@angular/core';
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { concatMap, debounceTime, from, of, tap, toArray } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { Cliente } from '../../../core/models/cliente.model';
import { Material, MaterialRequest } from '../../../core/models/material.model';
import { Presupuesto, PresupuestoItemRequest } from '../../../core/models/presupuesto.model';
import { PresupuestoIaBorradorResponse, PresupuestoIaRequestError } from '../../../core/models/presupuesto-ia.model';
import { ClienteService } from '../../../core/services/cliente.service';
import { MaterialService } from '../../../core/services/material.service';
import { PresupuestoService } from '../../../core/services/presupuesto.service';
import { PresupuestoIaService } from '../../../core/services/presupuesto-ia.service';
import { calcularPresupuestoCostes } from '../../../core/utils/presupuesto-costes.util';
import { EnviarPresupuestoComponent } from '../enviar-presupuesto/enviar-presupuesto.component';

const BORRADOR_LEGACY_KEY = 'presupuesto_rapido_borrador_v1';
const BORRADOR_KEY_PREFIX = 'presupuesto_rapido_borrador';
const BORRADOR_VERSION = 2;
const BORRADOR_TTL_MS = 7 * 24 * 60 * 60 * 1000;

function parseDecimal(value: unknown): number | null {
  if (typeof value === 'number') return Number.isFinite(value) ? value : null;
  if (typeof value !== 'string') return null;
  const raw = value.trim();
  if (!/^[+-]?(?:\d+(?:[.,]\d*)?|[.,]\d+)$/.test(raw)) return null;
  const parsed = Number(raw.replace(',', '.'));
  return Number.isFinite(parsed) ? parsed : null;
}

function decimalMin(minimum: number) {
  return (control: { value: unknown }) => {
    if (control.value == null || control.value === '') return null;
    const value = parseDecimal(control.value);
    return value == null ? { decimal: true } : value < minimum ? { min: { min: minimum, actual: value } } : null;
  };
}

@Component({
  selector: 'app-presupuesto-rapido',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatCheckboxModule,
    MatSnackBarModule,
    MatIconModule,
    MatTooltipModule,
    MatRadioModule,
    FormsModule,
    TranslateModule,
    EnviarPresupuestoComponent,
  ],
  template: `
    <div class="rapido-wrap">
      <mat-card>
        <mat-card-header>
          <mat-card-title>{{ 'budQuick.title' | translate }}</mat-card-title>
          <mat-card-subtitle>{{ 'budQuick.subtitle' | translate }}</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="crearYEnviar()">
            <section class="cliente-block" aria-labelledby="cliente-title">
              <h2 id="cliente-title" class="section-title">{{ 'budQuick.clientLabel' | translate }}</h2>
              <mat-radio-group
                [(ngModel)]="clienteModo"
                [ngModelOptions]="{standalone: true}"
                (ngModelChange)="onClienteModoChange($event)"
                class="modo-radios"
                [attr.aria-label]="'budQuick.clientLabel' | translate"
              >
                <mat-radio-button value="existente">{{ 'budQuick.modeExisting' | translate }}</mat-radio-button>
                <mat-radio-button value="nuevo">{{ 'budQuick.modeNewQuick' | translate }}</mat-radio-button>
              </mat-radio-group>
              @if (clienteModo === 'existente') {
                <mat-form-field appearance="outline" class="full">
                  <mat-label>{{ 'budQuick.searchClient' | translate }}</mat-label>
                  <input matInput [(ngModel)]="busquedaCliente" [ngModelOptions]="{standalone: true}" autocomplete="off" />
                  <mat-icon matSuffix>search</mat-icon>
                </mat-form-field>
                <mat-form-field appearance="outline" class="full">
                  <mat-label>{{ 'budQuick.clientSelect' | translate }}</mat-label>
                  <mat-select formControlName="clienteId">
                    @for (c of clientesFiltrados(); track c.id) {
                      <mat-option [value]="c.id">{{ c.nombre }}</mat-option>
                    }
                  </mat-select>
                </mat-form-field>
              } @else {
                <div class="nuevo-rapido">
                  <mat-form-field appearance="outline" class="nombre-nuevo">
                    <mat-label>{{ 'budQuick.quickClientName' | translate }}</mat-label>
                    <input matInput [(ngModel)]="nombreClienteNuevo" [ngModelOptions]="{standalone: true}" autocomplete="name" />
                  </mat-form-field>
                  <button type="button" mat-stroked-button (click)="crearClienteRapido()" [disabled]="!auth.canMutate() || !nombreClienteNuevo.trim()">
                    {{ 'budQuick.createUse' | translate }}
                  </button>
                </div>
              }
            </section>

            <section class="section ia-section" aria-labelledby="ia-title">
              <h2 id="ia-title" class="section-title">{{ 'budQuick.ai.title' | translate }}</h2>
              <mat-form-field appearance="outline" class="full">
                <mat-label>{{ 'budQuick.ai.descriptionLabel' | translate }}</mat-label>
                <textarea matInput [(ngModel)]="textoObraIa" [ngModelOptions]="{standalone: true}"
                  maxlength="8000" rows="4" aria-describedby="ia-char-count ia-status"></textarea>
              </mat-form-field>
              <div class="ai-actions">
                <span id="ia-char-count" class="char-count">{{ 'budQuick.ai.charCount' | translate:{count: textoObraIa.length} }}</span>
                <button type="button" mat-raised-button color="primary" class="ai-generate"
                  (click)="generarConIa()" [disabled]="iaLoading || iaDesactivada || !textoObraIa.trim() || textoObraIa.length > 8000 || !auth.canMutate()"
                  [attr.aria-disabled]="iaLoading || iaDesactivada || !textoObraIa.trim() || textoObraIa.length > 8000 || !auth.canMutate()">
                  <mat-icon>{{ iaLoading ? 'sync' : 'auto_awesome' }}</mat-icon>
                  {{ (iaLoading ? 'budQuick.ai.generating' : 'budQuick.ai.generate') | translate }}
                </button>
              </div>
              <p #iaStatus id="ia-status" class="ai-status" [class.error-message]="!!iaErrorMessage"
                [hidden]="!iaStatusMessage" [attr.role]="iaErrorMessage ? 'alert' : 'status'"
                aria-live="polite" aria-atomic="true" tabindex="-1">{{ iaStatusMessage }}</p>
              @if (clienteSugerido) {
                <div class="client-suggestion" aria-live="polite">
                  <p><strong>{{ 'budQuick.ai.clientSuggestion' | translate }}</strong> {{ clienteSugerido.nombre }}</p>
                  @if (clienteSugerido.telefono) { <p>{{ clienteSugerido.telefono }}</p> }
                  @if (clientesCoincidentes.length) {
                    <div class="suggested-clients">
                      @for (clienteMatch of clientesCoincidentes; track clienteMatch.id) {
                        <button type="button" mat-stroked-button (click)="usarClienteSugerido(clienteMatch)">
                          {{ 'budQuick.ai.useClient' | translate }}: {{ clienteMatch.nombre }}
                        </button>
                      }
                    </div>
                  }
                  @if (clienteSugerido.nombre) {
                    <button type="button" mat-stroked-button (click)="crearClienteSugerido()"
                      [disabled]="!auth.canMutate() || creandoClienteSugerido">
                      {{ 'budQuick.ai.createProvisional' | translate }}
                    </button>
                  }
                </div>
              }
            </section>

            <section class="section materiales-section" aria-labelledby="materiales-title">
              <h2 id="materiales-title" class="section-title">{{ 'budQuick.materialsTitle' | translate }}</h2>
              <mat-form-field appearance="outline" class="full search-field">
                <mat-label>{{ 'budQuick.searchCatalog' | translate }}</mat-label>
                <input matInput [(ngModel)]="busquedaMaterial" [ngModelOptions]="{standalone: true}" autocomplete="off" />
                <mat-icon matSuffix>search</mat-icon>
              </mat-form-field>
              @if (materialesTop.length) {
                <div class="top-used" [attr.aria-label]="'budQuick.topUsed' | translate">
                  <span class="top-label">{{ 'budQuick.topUsed' | translate }}</span>
                  @for (m of topUsadosFiltrados(); track m.id) {
                    <button type="button" mat-stroked-button class="top-chip" (click)="seleccionarTop(m)">{{ m.nombre }}</button>
                  }
                </div>
              }
              @if (catalogoLoadFailed) {
                <p class="error-message" role="alert">{{ 'budQuick.catalogLoadError' | translate }}</p>
                <button type="button" mat-stroked-button class="retry-btn" (click)="cargarCatalogo()">{{ 'budQuick.retry' | translate }}</button>
              }
              <div formArrayName="materialItems" class="lines-block">
                @for (line of materialItems.controls; track line; let i = $index) {
                  <div [formGroupName]="i" class="line-card">
                    <mat-form-field appearance="outline" class="full">
                      <mat-label>{{ 'budQuick.material' | translate }}</mat-label>
                      <mat-select formControlName="materialId" (selectionChange)="onMaterial(i, $event.value)">
                        <mat-option [value]="null">{{ 'budQuick.selectMat' | translate }}</mat-option>
                        @for (m of catalogoFiltrado(); track m.id) {
                          <mat-option [value]="m.id">{{ m.nombre }} · {{ m.precioUnitario | number:'1.2-2' }} € / {{ m.unidadMedida }}</mat-option>
                        }
                      </mat-select>
                    </mat-form-field>
                    <mat-form-field appearance="outline" class="full">
                      <mat-label>{{ 'budQuick.description' | translate }}</mat-label>
                      <input matInput formControlName="descripcion" autocomplete="off" />
                    </mat-form-field>
                    <div class="numeric-fields">
                      <mat-form-field appearance="outline">
                        <mat-label>{{ 'budQuick.qtyShort' | translate }}</mat-label>
                        <input matInput type="text" inputmode="decimal" formControlName="cantidad" autocomplete="off" />
                      </mat-form-field>
                      <mat-form-field appearance="outline">
                        <mat-label>{{ 'budQuick.unitShort' | translate }}</mat-label>
                        <input matInput type="text" inputmode="decimal" formControlName="precioUnitario" autocomplete="off" />
                      </mat-form-field>
                    </div>
                    <button mat-stroked-button type="button" class="remove-btn" (click)="removeMaterialLine(i)" [disabled]="totalLineCount() <= 1">
                      <mat-icon>delete_outline</mat-icon>{{ 'budQuick.removeLine' | translate }}
                    </button>
                  </div>
                }
              </div>
              <button mat-stroked-button type="button" class="add-line" (click)="addMaterialLine()">
                <mat-icon>add</mat-icon>{{ 'budQuick.addMaterial' | translate }}
              </button>
            </section>

            <section class="section tareas-section" aria-labelledby="manual-title">
              <h2 id="manual-title" class="section-title">{{ 'budQuick.manualTitle' | translate }}</h2>
              <div formArrayName="manualItems" class="lines-block">
                @for (line of manualItems.controls; track line; let i = $index) {
                  <div [formGroupName]="i" class="line-card" [class.ai-review-card]="requiereRevisionVisual(line)">
                    @if (line.get('iaSugerida')?.value) {
                      <div class="ai-line-status" aria-live="polite">
                        <span class="ai-label"><mat-icon aria-hidden="true">auto_awesome</mat-icon>
                          {{ (line.get('iaRevisada')?.value ? 'budQuick.ai.reviewed' : 'budQuick.ai.suggested') | translate }}
                        </span>
                        @if (line.get('confianza')?.value === 'baja') {
                          <span class="review-label"><mat-icon aria-hidden="true">warning_amber</mat-icon>{{ 'budQuick.ai.lowConfidence' | translate }}</span>
                        }
                      </div>
                      @if (line.get('materialId')?.value) {
                        <p class="ia-material-name"><mat-icon aria-hidden="true">inventory_2</mat-icon>
                          <span>{{ 'budQuick.ai.associatedMaterial' | translate }}: <strong>{{ line.get('materialNombre')?.value }}</strong></span>
                        </p>
                      }
                      <mat-form-field appearance="outline" class="full">
                        <mat-label>{{ 'budQuick.ai.changeMaterial' | translate }}</mat-label>
                        <mat-select formControlName="materialId" (selectionChange)="onIaMaterialChange(i, $event.value)">
                          <mat-option [value]="null">{{ 'budQuick.ai.freeItem' | translate }}</mat-option>
                          @if (line.get('materialId')?.value && !materialEstaEnCatalogo(line.get('materialId')?.value)) {
                            <mat-option [value]="line.get('materialId')?.value">{{ line.get('materialNombre')?.value }}</mat-option>
                          }
                          @for (m of catalogoFiltrado(); track m.id) {
                            <mat-option [value]="m.id">{{ m.nombre }} · {{ m.precioUnitario | number:'1.2-2' }} € / {{ m.unidadMedida }}</mat-option>
                          }
                        </mat-select>
                      </mat-form-field>
                      @if (line.get('faltaPrecio')?.value) {
                        <p class="review-label"><mat-icon aria-hidden="true">warning_amber</mat-icon>{{ 'budQuick.ai.missingPrice' | translate }}</p>
                      }
                      @if (line.get('cantidadDudosa')?.value) {
                        <p class="review-label"><mat-icon aria-hidden="true">warning_amber</mat-icon>{{ 'budQuick.ai.missingQuantity' | translate }}</p>
                      }
                    }
                    <mat-form-field appearance="outline" class="full">
                      <mat-label>{{ 'budQuick.description' | translate }}</mat-label>
                      <input matInput formControlName="tareaManual" autocomplete="off" />
                    </mat-form-field>
                    <mat-form-field appearance="outline" class="full">
                      <mat-label>{{ 'budQuick.unitMeasure' | translate }}</mat-label>
                      <input matInput formControlName="unidadMedida" autocomplete="off" />
                    </mat-form-field>
                    <div class="numeric-fields">
                      <mat-form-field appearance="outline">
                        <mat-label>{{ 'budQuick.qtyShort' | translate }}</mat-label>
                        <input matInput type="text" inputmode="decimal" formControlName="cantidad" autocomplete="off" (input)="actualizarCantidadIa(i)" />
                      </mat-form-field>
                      <mat-form-field appearance="outline">
                        <mat-label>{{ 'budQuick.unitShort' | translate }}</mat-label>
                        <input matInput type="text" inputmode="decimal" formControlName="precioUnitario" autocomplete="off" (input)="actualizarPrecioIa(i)" />
                      </mat-form-field>
                    </div>
                    <mat-checkbox formControlName="guardarEnCatalogo">{{ 'budQuick.saveToCatalog' | translate }}</mat-checkbox>
                    @if (line.get('iaSugerida')?.value && !line.get('iaRevisada')?.value) {
                      <button mat-stroked-button type="button" class="confirm-ai-btn" (click)="confirmarSugerenciaIa(i)">
                        {{ 'budQuick.ai.confirmSuggestion' | translate }}
                      </button>
                    }
                    <button mat-stroked-button type="button" class="remove-btn" (click)="removeManualLine(i)" [disabled]="totalLineCount() <= 1">
                      <mat-icon>delete_outline</mat-icon>{{ 'budQuick.removeLine' | translate }}
                    </button>
                  </div>
                }
              </div>
              <button mat-stroked-button type="button" class="add-line" (click)="addManualLine()">
                <mat-icon>add</mat-icon>{{ 'budQuick.addManual' | translate }}
              </button>
            </section>

            <mat-checkbox formControlName="ivaHabilitado">{{ 'budQuick.vatCheck' | translate }}</mat-checkbox>
            <mat-form-field appearance="outline" class="full notes-field">
              <mat-label>{{ 'budQuick.notes' | translate }}</mat-label>
              <textarea matInput formControlName="notaAdicional" rows="2" maxlength="1000"></textarea>
            </mat-form-field>
            @if (clientesLoadFailed) {
              <p class="error-message" role="alert">{{ 'budQuick.clientsLoadError' | translate }}</p>
              <button type="button" mat-stroked-button class="retry-btn" (click)="cargarClientes()">{{ 'budQuick.retry' | translate }}</button>
            }
            @if (errorMessage) {
              <p class="error-message" role="alert">{{ errorMessage }}</p>
            }
          </form>

          @if (presupuestoCreado) {
            <app-enviar-presupuesto [presupuesto]="presupuestoCreado" (enviado)="presupuestoCreado = $event" />
          }
        </mat-card-content>
      </mat-card>
    </div>
    @if (!presupuestoCreado) {
      <div class="submit-bar">
        <div class="submit-details">
          @if (pendientesIa().precio > 0 || pendientesIa().cantidad > 0 || pendientesIa().revision > 0) {
            <p class="pending-ai-summary" role="status" aria-live="polite" aria-atomic="true">
              {{ 'budQuick.ai.pendingSummary' | translate:{price: pendientesIa().precio, quantity: pendientesIa().cantidad, review: pendientesIa().revision} }}
            </p>
          }
          @if (cantidadSegurasPorConfirmar() > 0) {
            <button mat-stroked-button type="button" class="confirm-ai-btn confirm-safe-btn" (click)="confirmarSugerenciasSeguras()">
              {{ 'budQuick.ai.confirmSafe' | translate:{count: cantidadSegurasPorConfirmar()} }}
            </button>
          }
          <div class="total-preview"><span>{{ 'budQuick.total' | translate }}</span><strong>{{ totalPreview() | number:'1.2-2' }} €</strong></div>
        </div>
        <button mat-raised-button color="primary" type="button" class="submit-btn" (click)="crearYEnviar()" [disabled]="!puedeCrear() || loading || !auth.canMutate()">
          @if (loading) { <mat-icon class="spin">sync</mat-icon> } @else { <mat-icon>send</mat-icon> }
          {{ 'budQuick.createAndSend' | translate }}
        </button>
      </div>
    }
  `,
  styles: [`
    .rapido-wrap { max-width: 720px; margin: 20px auto 0; padding: 0 16px 112px; }
    .cliente-block { margin-bottom: 16px; }
    .modo-radios { display: flex; flex-wrap: wrap; gap: 12px; margin: 8px 0 12px; }
    .nuevo-rapido { display: flex; flex-wrap: wrap; gap: 12px; align-items: flex-start; }
    .nombre-nuevo { flex: 1; min-width: 200px; }
    .full { width: 100%; display: block; }
    .section { margin: 8px 0 18px; padding: 12px; border-radius: var(--app-radius-md, 12px); }
    .section-title { font-size: 1rem; font-weight: 600; margin: 0 0 10px; }
    .materiales-section { border: 1px solid rgba(30, 58, 138, .16); background: rgba(30, 58, 138, .03); }
    .tareas-section { border: 1px solid rgba(180, 83, 9, .22); background: rgba(180, 83, 9, .06); }
    .ia-section { border: 1px solid #64748b; background: #f8fafc; }
    .ai-actions { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 12px; }
    .ai-generate, .confirm-ai-btn { min-height: 48px; }
    .char-count { color: var(--app-text-secondary, #475569); font-size: .9rem; }
    .ai-status { padding: 10px 12px; border-radius: 8px; background: #e0f2fe; color: #0c4a6e; }
    .ai-status.error-message { background: #fef2f2; color: #991b1b; }
    .client-suggestion { margin-top: 12px; padding: 12px; border: 1px solid #64748b; border-radius: 10px; overflow-wrap: anywhere; }
    .suggested-clients { display: flex; flex-wrap: wrap; gap: 8px; margin: 8px 0; }
    .ai-review-card { border: 2px solid #a16207; background: #fffbeb; }
    .ai-line-status { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-bottom: 8px; }
    .ai-label, .review-label, .ia-material-name { display: inline-flex; align-items: center; gap: 6px; }
    .ai-label, .review-label { min-height: 44px; }
    .review-label { color: #713f12; font-weight: 700; }
    .ia-material-name { margin: 6px 0 10px; overflow-wrap: anywhere; }
    .pending-ai-summary { padding: 10px; border-left: 4px solid #a16207; background: #fffbeb; font-weight: 700; }
    .submit-details { display: flex; flex-wrap: wrap; align-items: center; gap: 8px 16px; }
    .submit-details .pending-ai-summary { max-width: min(52vw, 440px); margin: 0; }
    .confirm-safe-btn { min-height: 48px; }
    .top-used { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin: 0 0 12px; }
    .top-label { width: 100%; font-size: 13px; color: var(--app-text-secondary, #64748b); }
    .top-chip, .add-line, .remove-btn { min-height: 44px; }
    .lines-block { display: flex; flex-direction: column; gap: 12px; }
    .line-card { padding: 10px; border-radius: 10px; background: var(--app-bg-page, #f8fafc); border: 1px solid var(--app-border, #e2e8f0); }
    .numeric-fields { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
    .numeric-fields mat-form-field { width: 100%; }
    .remove-btn { display: flex; margin: 2px 0 0 auto; align-items: center; gap: 4px; }
    .add-line { margin-top: 10px; }
    .notes-field { margin-top: 12px; }
    .error-message { color: #b91c1c; font-weight: 600; }
    .retry-btn { min-height: 44px; margin: 0 0 10px; }
    .submit-bar { position: fixed; z-index: 100; bottom: 0; left: 0; right: 0; display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 10px max(16px, calc((100vw - 720px) / 2)) calc(10px + env(safe-area-inset-bottom)); background: var(--app-bg-card, #fff); border-top: 1px solid var(--app-border, #cbd5e1); box-shadow: 0 -4px 14px rgba(15, 23, 42, .12); }
    .total-preview { display: flex; flex-direction: column; font-size: 13px; color: var(--app-text-secondary, #64748b); }
    .total-preview strong { font-size: 18px; color: var(--app-text-primary, #0f172a); }
    .submit-btn { min-height: 52px; padding: 0 18px; font-weight: 700; }
    .spin { animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
    @media (max-width: 700px) { .rapido-wrap { margin-top: 10px; padding: 0 10px 120px; } .submit-bar { padding-left: 12px; padding-right: 12px; } .submit-btn { min-height: 52px; } .section { padding: 10px; } }
  `],
})
export class PresupuestoRapidoComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private draftUserId: number | null = null;
  form = this.fb.group({
    clienteId: [null as number | null],
    materialItems: this.fb.array([this.createMaterialLine()]),
    manualItems: this.fb.array([]),
    ivaHabilitado: [true],
    notaAdicional: [''],
  });
  clientes: Cliente[] = [];
  materiales: Material[] = [];
  materialesTop: Material[] = [];
  busquedaMaterial = '';
  busquedaCliente = '';
  loading = false;
  clientesLoadFailed = false;
  catalogoLoadFailed = false;
  errorMessage = '';
  clienteModo: 'existente' | 'nuevo' = 'existente';
  nombreClienteNuevo = '';
  presupuestoCreado: Presupuesto | null = null;
  textoObraIa = '';
  iaLoading = false;
  iaDesactivada = false;
  iaStatusMessage = '';
  iaErrorMessage = '';
  creandoClienteSugerido = false;
  clienteSugerido: { nombre: string; telefono: string } | null = null;
  clientesCoincidentes: Cliente[] = [];
  @ViewChild('iaStatus') private iaStatusElement?: ElementRef<HTMLElement>;

  constructor(
    private fb: FormBuilder,
    public auth: AuthService,
    private presupuestoService: PresupuestoService,
    private clienteService: ClienteService,
    private materialService: MaterialService,
    private presupuestoIaService: PresupuestoIaService,
    private snackBar: MatSnackBar,
    private translate: TranslateService,
  ) {
    effect(() => {
      const token = this.auth.token();
      if (!token && this.draftUserId != null) {
        this.eliminarBorrador();
        this.draftUserId = null;
      }
    });
  }

  get materialItems(): FormArray { return this.form.get('materialItems') as FormArray; }
  get manualItems(): FormArray { return this.form.get('manualItems') as FormArray; }
  totalLineCount(): number { return this.materialItems.length + this.manualItems.length; }

  ngOnInit(): void {
    this.form.valueChanges.pipe(debounceTime(250), takeUntilDestroyed(this.destroyRef)).subscribe(() => this.guardarBorrador());
    const userId = this.auth.user()?.id;
    if (userId != null) {
      this.activarBorrador(userId);
    } else {
      this.auth.refreshUser().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((user) => {
        if (user?.id != null && this.auth.token()) this.activarBorrador(user.id);
      });
    }
    this.cargarClientes();
    this.cargarCatalogo();
  }

  private activarBorrador(userId: number): void {
    this.draftUserId = userId;
    try { window.localStorage.removeItem(BORRADOR_LEGACY_KEY); } catch { /* Limpieza opcional del formato compartido anterior. */ }
    this.restaurarBorrador();
  }

  cargarClientes(): void {
    this.clienteService.getAll().subscribe({ next: (c) => { this.clientes = c; this.clientesLoadFailed = false; }, error: () => this.clientesLoadFailed = true });
  }

  cargarCatalogo(): void {
    this.materialService.getAll().subscribe({ next: (m) => { this.materiales = m; this.catalogoLoadFailed = false; }, error: () => this.catalogoLoadFailed = true });
    this.materialService.getTopUsados().subscribe({ next: (m) => this.materialesTop = m.slice(0, 5), error: () => this.materialesTop = [] });
  }

  clientesFiltrados(): Cliente[] {
    const query = this.busquedaCliente.trim().toLocaleLowerCase();
    return query ? this.clientes.filter((c) => c.nombre.toLocaleLowerCase().includes(query)) : this.clientes;
  }

  topUsadosFiltrados(): Material[] { return this.materialesTop.filter((m) => this.coincideBusqueda(m)); }
  catalogoFiltrado(): Material[] {
    const query = this.busquedaMaterial.trim().toLocaleLowerCase();
    if (!query) return this.materiales;
    const terms = query.split(/\s+/).filter(Boolean);
    return this.materiales.filter((m) => terms.every((term) => `${m.nombre} ${m.unidadMedida}`.toLocaleLowerCase().includes(term)));
  }
  private coincideBusqueda(m: Material): boolean {
    const query = this.busquedaMaterial.trim().toLocaleLowerCase();
    return !query || query.split(/\s+/).filter(Boolean).every((term) => `${m.nombre} ${m.unidadMedida}`.toLocaleLowerCase().includes(term));
  }

  nombreClienteSeleccionado(): string {
    const id = this.form.controls.clienteId.value;
    return this.clientes.find((c) => c.id === id)?.nombre ?? '';
  }

  onClienteModoChange(modo: 'existente' | 'nuevo'): void {
    if (modo === 'nuevo') this.form.patchValue({ clienteId: null });
  }

  crearClienteRapido(): void {
    const nombre = this.nombreClienteNuevo.trim();
    if (!nombre) return;
    this.clienteService.createProvisional({ nombre }).subscribe({
      next: (cliente) => {
        this.clientes = [...this.clientes, cliente].sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'));
        this.form.patchValue({ clienteId: cliente.id });
        this.clienteModo = 'existente';
        this.nombreClienteNuevo = '';
      },
      error: () => this.errorMessage = this.translate.instant('budQuick.clientCreateError'),
    });
  }

  private createMaterialLine(): FormGroup {
    return this.fb.group({
      materialId: [null as number | null],
      descripcion: [''],
      cantidad: [1, [Validators.required, decimalMin(0.001)]],
      precioUnitario: [0, [Validators.required, decimalMin(0)]],
    });
  }
  private createManualLine(): FormGroup {
    return this.fb.group({
      tareaManual: ['', Validators.required],
      unidadMedida: ['ud'],
      cantidad: [1, [Validators.required, decimalMin(0.001)]],
      precioUnitario: [0, [Validators.required, decimalMin(0)]],
      guardarEnCatalogo: [false],
      materialIdCreado: [null as number | null],
      materialId: [null as number | null],
      materialNombre: [''],
      iaSugerida: [false],
      iaRevisada: [false],
      confianza: ['alta'],
      faltaPrecio: [false],
      cantidadDudosa: [false],
    });
  }

  addMaterialLine(): void { this.materialItems.push(this.createMaterialLine()); }
  addManualLine(): void { this.manualItems.push(this.createManualLine()); }
  removeMaterialLine(index: number): void { if (this.totalLineCount() > 1) this.materialItems.removeAt(index); }
  removeManualLine(index: number): void { if (this.totalLineCount() > 1) this.manualItems.removeAt(index); }

  seleccionarTop(material: Material): void {
    let index = this.materialItems.controls.findIndex((line) => !line.get('materialId')?.value);
    if (index < 0) { this.addMaterialLine(); index = this.materialItems.length - 1; }
    this.materialItems.at(index).get('materialId')?.setValue(material.id);
    this.onMaterial(index, material.id);
  }

  onMaterial(index: number, id: number | null): void {
    if (id == null) return;
    const material = this.materiales.find((m) => m.id === id);
    if (!material) return;
    this.materialItems.at(index).patchValue({ descripcion: material.nombre, precioUnitario: material.precioUnitario });
  }

  totalPreview(): number {
    const items = [
      ...this.materialItems.controls.map((line) => line.getRawValue()).filter((v) => v.materialId),
      ...this.manualItems.controls.map((line) => line.getRawValue()).filter((v) => String(v.tareaManual ?? '').trim()),
    ];
    return calcularPresupuestoCostes({
      items: items.map((v) => ({ cantidad: parseDecimal(v.cantidad) ?? 0, precioUnitario: parseDecimal(v.precioUnitario) ?? 0 })),
      ivaHabilitado: this.form.controls.ivaHabilitado.value !== false,
    }).total;
  }

  puedeCrear(): boolean {
    if (this.form.controls.clienteId.value == null || this.loading || this.presupuestoCreado) return false;
    const pending = this.pendientesIa();
    if (pending.precio > 0 || pending.cantidad > 0 || pending.revision > 0) return false;
    let activeCount = 0;
    for (const control of this.materialItems.controls) {
      const values = control.getRawValue();
      if (!values.materialId) continue;
      activeCount++;
      if (!this.validNumericLine(values.cantidad, values.precioUnitario)) return false;
    }
    for (const control of this.manualItems.controls) {
      const values = control.getRawValue();
      if (!String(values.tareaManual ?? '').trim()) continue;
      activeCount++;
      if (!this.validNumericLine(values.cantidad, values.precioUnitario)) return false;
    }
    return activeCount > 0;
  }

  private validNumericLine(quantity: unknown, price: unknown): boolean {
    const parsedQuantity = parseDecimal(quantity);
    const parsedPrice = parseDecimal(price);
    return parsedQuantity != null && parsedQuantity >= 0.001 && parsedPrice != null && parsedPrice >= 0;
  }

  pendientesIa(): { precio: number; cantidad: number; revision: number } {
    let precio = 0;
    let cantidad = 0;
    let revision = 0;
    for (const control of this.manualItems.controls) {
      const values = control.getRawValue();
      if (!values.iaSugerida) continue;
      if (!values.iaRevisada) revision++;
      const parsedPrice = parseDecimal(values.precioUnitario);
      const parsedQuantity = parseDecimal(values.cantidad);
      if (values.faltaPrecio || parsedPrice == null || parsedPrice <= 0) precio++;
      if (values.cantidadDudosa || parsedQuantity == null || parsedQuantity <= 0) cantidad++;
    }
    return { precio, cantidad, revision };
  }

  cantidadSegurasPorConfirmar(): number {
    return this.manualItems.controls.filter((control) => {
      const values = control.getRawValue();
      const materialId = values.materialId;
      return values.iaSugerida && !values.iaRevisada &&
        values.confianza === 'alta' &&
        typeof materialId === 'number' && this.materiales.some((material) => material.id === materialId) &&
        !values.faltaPrecio && !values.cantidadDudosa &&
        (parseDecimal(values.precioUnitario) ?? 0) > 0 &&
        this.validNumericLine(values.cantidad, values.precioUnitario);
    }).length;
  }

  confirmarSugerenciasSeguras(): void {
    for (const control of this.manualItems.controls) {
      const values = control.getRawValue();
      const materialId = values.materialId;
      const safe = values.iaSugerida && !values.iaRevisada &&
        values.confianza === 'alta' &&
        typeof materialId === 'number' && this.materiales.some((material) => material.id === materialId) &&
        !values.faltaPrecio && !values.cantidadDudosa &&
        (parseDecimal(values.precioUnitario) ?? 0) > 0 &&
        this.validNumericLine(values.cantidad, values.precioUnitario);
      if (safe) control.get('iaRevisada')?.setValue(true);
    }
  }

  requiereRevisionVisual(line: AbstractControl): boolean {
    return line.get('iaSugerida')?.value === true &&
      (line.get('faltaPrecio')?.value === true || line.get('cantidadDudosa')?.value === true);
  }

  generarConIa(): void {
    const texto = this.textoObraIa.trim();
    if (!texto || texto.length > 8000 || this.iaLoading || this.iaDesactivada || !this.auth.canMutate()) return;
    if (this.hayTrabajoEditado()) {
      const confirm = window.confirm(this.translate.instant('budQuick.ai.confirmOverwrite'));
      if (!confirm) return;
    }

    this.iaLoading = true;
    this.iaErrorMessage = '';
    this.iaStatusMessage = this.translate.instant('budQuick.ai.progress');
    const clienteId = this.form.controls.clienteId.value;
    this.presupuestoIaService.generarBorrador({ texto, ...(clienteId == null ? {} : { clienteId }) })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (draft) => {
          this.cargarBorradorIa(draft);
          this.iaStatusMessage = this.translate.instant('budQuick.ai.draftReady');
          this.enfocarEstadoIa();
        },
        error: (error: unknown) => {
          this.iaLoading = false;
          this.iaErrorMessage = this.mensajeErrorIa(error);
          this.iaStatusMessage = this.iaErrorMessage;
          this.enfocarEstadoIa();
        },
        complete: () => this.iaLoading = false,
      });
  }

  private cargarBorradorIa(draft: PresupuestoIaBorradorResponse): void {
    this.materialItems.clear();
    this.materialItems.push(this.createMaterialLine());
    this.manualItems.clear();
    for (const item of draft.items) {
      const line = this.createManualLine();
      line.patchValue({
        tareaManual: item.tareaManual,
        unidadMedida: item.unidad ?? 'ud',
        cantidad: item.cantidad,
        precioUnitario: item.precioUnitario,
        materialId: item.materialId,
        materialNombre: item.materialNombre ?? '',
        iaSugerida: true,
        iaRevisada: false,
        confianza: item.confianza,
        faltaPrecio: item.faltaPrecio || item.precioUnitario <= 0,
        cantidadDudosa: item.cantidadDudosa || item.cantidad == null || item.cantidad <= 0,
        guardarEnCatalogo: false,
      });
      this.manualItems.push(line);
    }
    if (draft.notaAdicional != null) this.form.controls.notaAdicional.setValue(draft.notaAdicional);
    this.proponerCliente(draft);
    this.iaLoading = false;
  }

  private proponerCliente(draft: PresupuestoIaBorradorResponse): void {
    const nombre = String(draft.clienteNombre ?? '').trim();
    const telefono = String(draft.clienteTelefono ?? '').trim();
    if (!nombre && !telefono) {
      this.clienteSugerido = null;
      this.clientesCoincidentes = [];
      return;
    }
    this.clienteSugerido = { nombre, telefono };
    const telefonoNormalizado = this.normalizarTelefono(telefono);
    const nombreNormalizado = this.normalizarNombre(nombre);
    this.clientesCoincidentes = this.clientes.filter((cliente) => {
      const mismoTelefono = !!telefonoNormalizado && this.normalizarTelefono(cliente.telefono) === telefonoNormalizado;
      const nombreCliente = this.normalizarNombre(cliente.nombre);
      const mismoNombre = !!nombreNormalizado && (nombreCliente === nombreNormalizado ||
        (nombreNormalizado.length >= 3 && nombreCliente.includes(nombreNormalizado)) ||
        (nombreCliente.length >= 3 && nombreNormalizado.includes(nombreCliente)));
      return mismoTelefono || mismoNombre;
    });
  }

  usarClienteSugerido(cliente: Cliente): void {
    this.form.controls.clienteId.setValue(cliente.id);
    this.clienteModo = 'existente';
    this.clienteSugerido = null;
    this.clientesCoincidentes = [];
  }

  crearClienteSugerido(): void {
    const suggestion = this.clienteSugerido;
    if (!suggestion?.nombre || !this.auth.canMutate() || this.creandoClienteSugerido) return;
    this.creandoClienteSugerido = true;
    this.clienteService.create({ nombre: suggestion.nombre, telefono: suggestion.telefono || undefined }).subscribe({
      next: (cliente) => {
        this.clientes = [...this.clientes, cliente].sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'));
        this.usarClienteSugerido(cliente);
        this.creandoClienteSugerido = false;
      },
      error: () => {
        this.creandoClienteSugerido = false;
        this.errorMessage = this.translate.instant('budQuick.clientCreateError');
      },
    });
  }

  onIaMaterialChange(index: number, rawId: number | null): void {
    const control = this.manualItems.at(index);
    if (rawId == null) {
      control.patchValue({ materialId: null, materialNombre: '', precioUnitario: 0, faltaPrecio: true });
      return;
    }
    const material = this.materiales.find((candidate) => candidate.id === Number(rawId));
    if (!material) return;
    control.patchValue({
      materialId: material.id,
      materialNombre: material.nombre,
      precioUnitario: material.precioUnitario,
      unidadMedida: material.unidadMedida || control.get('unidadMedida')?.value,
      faltaPrecio: material.precioUnitario <= 0,
    });
  }

  materialEstaEnCatalogo(id: number | null | undefined): boolean {
    return typeof id === 'number' && this.materiales.some((material) => material.id === id);
  }

  actualizarPrecioIa(index: number): void {
    const control = this.manualItems.at(index);
    if (!control.get('iaSugerida')?.value) return;
    const price = parseDecimal(control.get('precioUnitario')?.value);
    control.patchValue({ faltaPrecio: price == null || price <= 0 }, { emitEvent: false });
  }

  actualizarCantidadIa(index: number): void {
    const control = this.manualItems.at(index);
    if (!control.get('iaSugerida')?.value) return;
    const quantity = parseDecimal(control.get('cantidad')?.value);
    control.patchValue({ cantidadDudosa: quantity == null || quantity <= 0 }, { emitEvent: false });
  }

  confirmarSugerenciaIa(index: number): void {
    this.manualItems.at(index).get('iaRevisada')?.setValue(true);
  }

  private hayTrabajoEditado(): boolean {
    const manualWork = this.manualItems.controls.some((control) => {
      const values = control.getRawValue();
      return !!String(values.tareaManual ?? '').trim();
    });
    return manualWork || this.materialItems.controls.some((control) => !!control.get('materialId')?.value);
  }

  private mensajeErrorIa(error: unknown): string {
    const kind = error instanceof PresupuestoIaRequestError ? error.kind : 'unknown';
    if (kind === 'disabled') this.iaDesactivada = true;
    const keys: Record<string, string> = {
      disabled: 'disabled',
      'quota-hourly': 'quotaHourly',
      'quota-daily': 'quotaDaily',
      'quota-attempts': 'quotaAttempts',
      'quota-provider': 'quotaProvider',
      provider: 'providerError',
      'too-long': 'tooLong',
      forbidden: 'forbidden',
      unauthorized: 'unauthorized',
      'invalid-request': 'invalidRequest',
      unknown: 'genericError',
    };
    return this.translate.instant(`budQuick.ai.${keys[kind] ?? 'genericError'}`);
  }

  private enfocarEstadoIa(): void {
    setTimeout(() => this.iaStatusElement?.nativeElement.focus(), 0);
  }

  private normalizarTelefono(value: string | null | undefined): string {
    return String(value ?? '').replace(/\D/g, '');
  }

  crearYEnviar(): void {
    this.errorMessage = '';
    if (!this.puedeCrear()) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading = true;
    const pending: Array<{ index: number; values: Record<string, unknown> }> = [];
    let reusedDuplicate = false;
    this.manualItems.controls
      .map((control, index) => ({ index, values: control.getRawValue() }))
      .filter(({ values }) => values.guardarEnCatalogo && !values.materialIdCreado && String(values.tareaManual ?? '').trim())
      .forEach(({ index, values }) => {
        const name = String(values.tareaManual).trim();
        const duplicate = this.materiales.find((material) => this.normalizarNombre(material.nombre) === this.normalizarNombre(name));
        if (duplicate) {
          this.manualItems.at(index).patchValue({ materialIdCreado: duplicate.id });
          reusedDuplicate = true;
          return;
        }
        const price = parseDecimal(values.precioUnitario);
        if (price === 0 && !window.confirm(this.translate.instant('budQuick.confirmZeroPrice'))) {
          this.manualItems.at(index).get('guardarEnCatalogo')?.setValue(false);
          return;
        }
        pending.push({ index, values });
      });
    if (reusedDuplicate) {
      this.snackBar.open(this.translate.instant('budQuick.catalogDuplicateReused'), this.translate.instant('common.close'), { duration: 4000 });
    }
    (pending.length
      ? from(pending).pipe(
          concatMap(({ index, values }) => this.materialService.create({
            nombre: String(values['tareaManual']).trim(),
            precioUnitario: parseDecimal(values['precioUnitario']) ?? 0,
            unidadMedida: String(values['unidadMedida'] || 'ud').trim(),
          } satisfies MaterialRequest).pipe(tap((material) => this.manualItems.at(index).get('materialIdCreado')?.setValue(material.id)))),
          toArray(),
        )
      : of([] as Material[])).subscribe({
      next: (created) => {
        const catalogIds = new Map<number, number>();
        pending.forEach(({ index }, i) => catalogIds.set(index, created[i].id));
        this.crearPresupuesto(catalogIds);
      },
      error: () => {
        this.loading = false;
        this.errorMessage = this.translate.instant('budQuick.catalogSaveError');
      },
    });
  }

  private buildItemsPayload(catalogIds = new Map<number, number>()): PresupuestoItemRequest[] {
    const out: PresupuestoItemRequest[] = [];
    for (const control of this.materialItems.controls) {
      const v = control.getRawValue();
      if (v.materialId) out.push({ materialId: v.materialId, cantidad: parseDecimal(v.cantidad) ?? 0, precioUnitario: parseDecimal(v.precioUnitario) ?? 0, aplicaIva: true, visiblePdf: true });
    }
    this.manualItems.controls.forEach((control, index) => {
      const v = control.getRawValue();
      const desc = String(v.tareaManual ?? '').trim();
      if (!desc) return;
      const common = { cantidad: parseDecimal(v.cantidad) ?? 0, precioUnitario: parseDecimal(v.precioUnitario) ?? 0, aplicaIva: true, visiblePdf: true };
      const materialId = catalogIds.get(index) ?? (typeof v.materialIdCreado === 'number' ? v.materialIdCreado :
        typeof v.materialId === 'number' ? v.materialId : undefined);
      out.push(materialId ? { ...common, materialId } : { ...common, tareaManual: desc });
    });
    return out;
  }

  private crearPresupuesto(catalogIds: Map<number, number>): void {
    const value = this.form.getRawValue();
    this.presupuestoService.create({
      clienteId: value.clienteId!,
      estado: 'Pendiente',
      ivaHabilitado: value.ivaHabilitado !== false,
      items: this.buildItemsPayload(catalogIds),
      notaAdicional: String(value.notaAdicional ?? '').trim() || undefined,
    }).subscribe({
      next: (presupuesto) => {
        this.presupuestoCreado = presupuesto;
        this.eliminarBorrador();
        this.loading = false;
        this.snackBar.open(this.translate.instant('snack.budgetCreated'), this.translate.instant('common.close'), { duration: 2500 });
      },
      error: () => {
        this.loading = false;
        this.errorMessage = this.translate.instant('snack.budgetCreateFail');
      },
    });
  }

  private guardarBorrador(): void {
    if (this.draftUserId == null) return;
    try {
      const v = this.form.getRawValue();
      // Se guardan solo los campos de trabajo necesarios para recuperar el presupuesto.
      window.localStorage.setItem(this.borradorKey(), JSON.stringify({
        version: BORRADOR_VERSION,
        ownerId: this.draftUserId,
        savedAt: Date.now(),
        data: {
          clienteId: v.clienteId,
          materialItems: v.materialItems,
          manualItems: v.manualItems,
          ivaHabilitado: v.ivaHabilitado,
          notaAdicional: v.notaAdicional,
        },
      }));
    } catch { /* El almacenamiento local puede estar desactivado o lleno. */ }
  }

  private restaurarBorrador(): void {
    if (this.draftUserId == null) return;
    const key = this.borradorKey();
    try {
      const raw = window.localStorage.getItem(key);
      if (!raw) return;
      const envelope = JSON.parse(raw) as Record<string, unknown>;
      const savedAt = typeof envelope['savedAt'] === 'number' ? envelope['savedAt'] : 0;
      if (envelope['version'] !== BORRADOR_VERSION || envelope['ownerId'] !== this.draftUserId ||
          !savedAt || Date.now() - savedAt > BORRADOR_TTL_MS || Date.now() < savedAt ||
          !envelope['data'] || typeof envelope['data'] !== 'object') {
        window.localStorage.removeItem(key);
        return;
      }
      const draft = envelope['data'] as Record<string, unknown>;
      if (Array.isArray(draft['materialItems']) && draft['materialItems'].length) {
        this.materialItems.clear();
        for (const row of draft['materialItems'].slice(0, 50)) {
          const v = row as Record<string, unknown>;
          this.materialItems.push(this.createMaterialLine());
          this.materialItems.at(this.materialItems.length - 1).patchValue({
            materialId: typeof v['materialId'] === 'number' ? v['materialId'] : null,
            descripcion: typeof v['descripcion'] === 'string' ? v['descripcion'].slice(0, 300) : '',
            cantidad: parseDecimal(v['cantidad']) ?? 1,
            precioUnitario: parseDecimal(v['precioUnitario']) ?? 0,
          });
        }
      }
      if (Array.isArray(draft['manualItems'])) {
        this.manualItems.clear();
        for (const row of draft['manualItems'].slice(0, 50)) {
          const v = row as Record<string, unknown>;
          this.manualItems.push(this.createManualLine());
          this.manualItems.at(this.manualItems.length - 1).patchValue({
            tareaManual: typeof v['tareaManual'] === 'string' ? v['tareaManual'].slice(0, 300) : '',
            unidadMedida: typeof v['unidadMedida'] === 'string' ? v['unidadMedida'].slice(0, 30) : 'ud',
            cantidad: parseDecimal(v['cantidad']) ?? 1,
            precioUnitario: parseDecimal(v['precioUnitario']) ?? 0,
            guardarEnCatalogo: v['guardarEnCatalogo'] === true,
            materialIdCreado: typeof v['materialIdCreado'] === 'number' ? v['materialIdCreado'] : null,
            materialId: typeof v['materialId'] === 'number' ? v['materialId'] : null,
            materialNombre: typeof v['materialNombre'] === 'string' ? v['materialNombre'].slice(0, 200) : '',
            iaSugerida: v['iaSugerida'] === true,
            iaRevisada: v['iaRevisada'] === true,
            confianza: v['confianza'] === 'baja' || v['confianza'] === 'media' ? v['confianza'] : 'alta',
            faltaPrecio: v['faltaPrecio'] === true,
            cantidadDudosa: v['cantidadDudosa'] === true,
          });
        }
      }
      this.form.patchValue({
        clienteId: typeof draft['clienteId'] === 'number' ? draft['clienteId'] : null,
        ivaHabilitado: draft['ivaHabilitado'] !== false,
        notaAdicional: typeof draft['notaAdicional'] === 'string' ? draft['notaAdicional'].slice(0, 1000) : '',
      });
    } catch {
      try { window.localStorage.removeItem(key); } catch { /* Un borrador corrupto se ignora. */ }
    }
  }

  private eliminarBorrador(): void {
    if (this.draftUserId == null) return;
    try { window.localStorage.removeItem(this.borradorKey()); } catch { /* El borrador ya no es necesario. */ }
  }

  private borradorKey(): string {
    return `${BORRADOR_KEY_PREFIX}_v${BORRADOR_VERSION}_${this.draftUserId}`;
  }

  private normalizarNombre(value: string): string {
    return value.normalize('NFKD').replace(/\p{M}/gu, '').trim().replace(/\s+/g, ' ').toLocaleLowerCase();
  }
}
