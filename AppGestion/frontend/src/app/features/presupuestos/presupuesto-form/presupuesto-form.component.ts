import { ChangeDetectorRef, Component, DestroyRef, ElementRef, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { forkJoin } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AbstractControl, FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatRadioModule } from '@angular/material/radio';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/auth/auth.service';
import { PresupuestoService } from '../../../core/services/presupuesto.service';
import { ClienteService } from '../../../core/services/cliente.service';
import { MaterialService } from '../../../core/services/material.service';
import { Cliente } from '../../../core/models/cliente.model';
import { Material } from '../../../core/models/material.model';
import { AnticipoResumen, PresupuestoItemRequest } from '../../../core/models/presupuesto.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CondicionesPresupuestoFormValue, PresupuestoCondicionDisponible } from '../../../core/models/presupuesto-condiciones.model';
import { CondicionesPresupuestoComponent } from '../condiciones-presupuesto/condiciones-presupuesto.component';
import { CalculadoraM2Component, CalculadoraResult } from '../calculadora-m2/calculadora-m2.component';
import { HintBannerComponent } from '../../../shared/hint-banner/hint-banner.component';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { calcularPresupuestoCostes } from '../../../core/utils/presupuesto-costes.util';
import {
  esSugerenciaPresupuestoIaSegura,
  evaluarPendientesPresupuestoIa,
  parsePresupuestoDecimal,
} from '../../../core/utils/presupuesto-ia-review.util';
import { PresupuestoIaRevisionDatos } from '../../../core/utils/presupuesto-ia-review.util';
import { EnviarPresupuestoComponent } from '../enviar-presupuesto/enviar-presupuesto.component';
import { PresupuestoIaBorradorResponse } from '../../../core/models/presupuesto-ia.model';
import { PresupuestoIaPanelComponent, PresupuestoIaPanelState } from '../../../shared/presupuesto-ia-panel/presupuesto-ia-panel.component';
import { PresupuestoIaPriceInfoComponent } from '../../../shared/presupuesto-ia-panel/presupuesto-ia-price-info.component';
import { PresupuestoSeguimientoComponent } from '../presupuesto-seguimiento/presupuesto-seguimiento.component';

@Component({
    selector: 'app-presupuesto-form',
    imports: [
        CommonModule,
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
        MatChipsModule,
        MatTooltipModule,
        MatRadioModule,
        MatDialogModule,
        FormsModule,
        CondicionesPresupuestoComponent,
        TranslateModule,
        HintBannerComponent,
        EnviarPresupuestoComponent,
        PresupuestoIaPanelComponent,
        PresupuestoIaPriceInfoComponent,
        PresupuestoSeguimientoComponent,
    ],
    template: `
    <div class="presupuesto-form">
      <mat-card>
        <mat-card-header>
          <mat-card-title>{{ isEdit ? ('budgetForm.edit' | translate) : ('budgetForm.new' | translate) }}</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          @if (creacionCompletada && presupuestoActual) {
            <section class="creacion-completada" role="status">
              <mat-icon>check_circle</mat-icon>
              <h2>{{ (isEdit ? 'snack.budgetUpdated' : 'snack.budgetSavedCreated') | translate }}</h2>
              <p>{{ 'budgetForm.createdShareHint' | translate }}</p>
              <app-enviar-presupuesto [presupuesto]="presupuestoActual" [mostrarOpcionesAlInicio]="true" (enviado)="presupuestoActual = $event" />
              <app-presupuesto-seguimiento
                [presupuestoId]="presupuestoActual.id"
                [enviadoAt]="presupuestoActual.enviadoAt"
                [canalEnvio]="presupuestoActual.canalEnvio"
                [alertCount]="presupuestoActual.seguimientoAvisosEnviados ?? 0"
                [lastAlertAt]="presupuestoActual.seguimientoUltimoAvisoAt"
                [silenced]="presupuestoActual.seguimientoSilenciado ?? false"
                [clientPhone]="presupuestoActual.clienteTelefono"
                [clientCountry]="presupuestoActual.clientePais"
                (resend)="abrirPanelEnvio()"
              />
              <button mat-stroked-button type="button" routerLink="/presupuestos">{{ 'budgetForm.backToBudgets' | translate }}</button>
            </section>
          } @else {
          @if (!isEdit) {
            <app-hint-banner
              storageKey="hint_presupuesto_form_v2"
              title="¿Cómo crear un presupuesto?"
              [steps]="[
                { icon: 'person', text: 'Elige o crea el cliente. Si ya lo tienes guardado, selecciónalo directamente.' },
                { icon: 'inventory_2', text: 'Añade materiales desde el catálogo — el precio se rellena solo. Para trabajos sin material, usa «Tarea manual».' },
                { icon: 'price_check', text: '', textKey: 'budgetForm.stepReviewPrices' }
              ]"
            />
          }
          @if (!isEdit) {
            <div class="section anticipo-section anticipo-cartel-solo">
              <h3>{{ 'budgetForm.depositCartelEditTitle' | translate }}</h3>
              <p class="hint-anticipo">
                {{ 'budgetForm.depositCartelEditHint' | translate }}
              </p>
            </div>
          }
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <!-- 1. Cliente y Estado -->
            <div class="form-row cliente-block">
              <div class="cliente-modo">
                <mat-label class="modo-label">{{ 'budgetForm.customerModeLabel' | translate }}</mat-label>
                <mat-radio-group
                  [(ngModel)]="clienteModo"
                  [ngModelOptions]="{standalone: true}"
                  (ngModelChange)="onClienteModoChange($event)"
                  class="modo-radios"
                >
                  <mat-radio-button value="existente">{{ 'budgetForm.modeExisting' | translate }}</mat-radio-button>
                  <mat-radio-button value="nuevo">{{ 'budgetForm.modeNew' | translate }}</mat-radio-button>
                </mat-radio-group>
              </div>
              @if (clienteModo === 'existente') {
                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>{{ 'budgetForm.customerLabel' | translate }}</mat-label>
                  <mat-select formControlName="clienteId" required>
                    <mat-select-trigger>
                      {{ nombreClienteSeleccionado() }}
                    </mat-select-trigger>
                    @for (c of clientes; track c.id) {
                      <mat-option [value]="c.id">
                        <span class="opt-line">
                          <span>{{ c.nombre }}</span>
                          @if (c.estadoCliente === 'PROVISIONAL') {
                            <span class="badge-fiscal">{{ 'budgetForm.provisionalBadge' | translate }}</span>
                          }
                        </span>
                      </mat-option>
                    }
                  </mat-select>
                  <mat-error>{{ 'budgetForm.pickCustomerErr' | translate }}</mat-error>
                </mat-form-field>
              } @else {
                <div class="nuevo-rapido">
                  <mat-form-field appearance="outline" class="nombre-nuevo">
                    <mat-label>{{ 'budgetForm.quickNameLabel' | translate }}</mat-label>
                    <input matInput [(ngModel)]="nombreClienteNuevo" [ngModelOptions]="{standalone: true}" [placeholder]="'budgetForm.quickNamePh' | translate" />
                  </mat-form-field>
                  <button type="button" mat-stroked-button color="primary" (click)="crearClienteRapido()"
                    [disabled]="!auth.canMutate() || !nombreClienteNuevo.trim()">
                    {{ 'budgetForm.createContinue' | translate }}
                  </button>
                </div>
              }
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>{{ 'budgetForm.statusLabel' | translate }}</mat-label>
                <mat-select formControlName="estado">
                  <mat-option value="Pendiente">{{ 'est.lbl.budPending' | translate }}</mat-option>
                  <mat-option value="Aceptado">{{ 'est.lbl.budAccepted' | translate }}</mat-option>
                  <mat-option value="En ejecución">{{ 'est.lbl.budInProgress' | translate }}</mat-option>
                  <mat-option value="Rechazado">{{ 'est.lbl.budRejected' | translate }}</mat-option>
                </mat-select>
              </mat-form-field>
              <mat-checkbox formControlName="ivaHabilitado">{{ 'budgetForm.vatIncluded' | translate }}</mat-checkbox>
            </div>

            <!-- 2. Materiales (primero, con línea visible al abrir) -->
            <div class="section materiales-section">
              <h3>{{ 'budgetForm.materialsTitle' | translate }}</h3>
              @if (topMateriales.length > 0) {
                <div class="top-materiales">
                  <span class="top-label">{{ 'budgetForm.topUsed' | translate }}</span>
                  @for (m of topMateriales; track m.id) {
                    <button type="button" mat-stroked-button class="chip-btn" (click)="addMaterialFromTop(m)">
                      {{ m.nombre }}
                    </button>
                  }
                </div>
              }
              <div formArrayName="materialItems">
                @for (item of materialItems.controls; track item; let i = $index) {
                  <div [formGroupName]="i" class="item-row material-row">
                    @if (showVisibilityColumn) {
                      <mat-checkbox formControlName="visiblePdf" [matTooltip]="'budgetForm.tooltipVisiblePdf' | translate" class="visibility-check"></mat-checkbox>
                    }
                    <mat-form-field appearance="outline" class="material-select">
                      <mat-label>{{ 'factForm.material' | translate }}</mat-label>
                      <mat-select formControlName="materialId" (selectionChange)="onMaterialSelect(i, $event.value)">
                        <mat-option [value]="null">{{ 'budgetForm.selectMaterialPlaceholder' | translate }}</mat-option>
                        @for (m of materiales; track m.id) {
                          <mat-option [value]="m.id">{{ m.nombre }} ({{ m.precioUnitario | number:'1.2-2' }} €)</mat-option>
                        }
                      </mat-select>
                    </mat-form-field>
                    <mat-form-field appearance="outline">
                      <mat-label>{{ 'factForm.description' | translate }}</mat-label>
                      <input matInput formControlName="tareaManual" [placeholder]="'budgetForm.materialDescPh' | translate">
                    </mat-form-field>
                    <mat-form-field appearance="outline" class="qty-with-calc">
                      <mat-label>{{ 'factForm.qty' | translate }}</mat-label>
                      <input matInput type="text" inputmode="decimal" formControlName="cantidad" autocomplete="off">
                      <button
                        matSuffix
                        mat-icon-button
                        type="button"
                        (click)="openCalculadora(materialItems, i)"
                        [matTooltip]="'budgetForm.calcM2Tooltip' | translate"
                        [attr.aria-label]="'budgetForm.calcM2Tooltip' | translate"
                      >
                        <mat-icon>calculate</mat-icon>
                      </button>
                    </mat-form-field>
                    <mat-form-field appearance="outline">
                      <mat-label>{{ 'factForm.unitPrice' | translate }}</mat-label>
                      <input matInput type="text" inputmode="decimal" formControlName="precioUnitario" autocomplete="off">
                    </mat-form-field>
                    <mat-checkbox formControlName="aplicaIva">{{ 'factForm.vatShort' | translate }}</mat-checkbox>
                    <button type="button" mat-icon-button color="warn" (click)="removeMaterialItem(i)" [matTooltip]="'cliList.tooltipDelete' | translate">
                      <mat-icon>delete</mat-icon>
                    </button>
                  </div>
                }
              </div>
              <button type="button" mat-stroked-button (click)="addMaterialItem()">
                <mat-icon>add</mat-icon>
                {{ 'budgetForm.addMaterial' | translate }}
              </button>
            </div>

            <!-- 3. Tareas manuales -->
            <div class="section tareas-section">
              <h3>{{ 'budgetForm.manualTasksTitle' | translate }}</h3>
              <app-hint-banner
                storageKey="hint_presupuesto_ia_form_v1"
                [title]="'budQuick.aiHelp.title' | translate"
                [steps]="[
                  { icon: 'edit_note', text: ('budQuick.aiHelp.step1' | translate) },
                  { icon: 'auto_awesome', text: ('budQuick.aiHelp.step2' | translate) },
                  { icon: 'fact_check', text: ('budQuick.aiHelp.step3' | translate) }
                ]"
                [note]="'budQuick.aiHelp.note' | translate"
              />
              <app-presupuesto-ia-panel [text]="textoObraIa" (textChange)="textoObraIa = $event"
                [clienteId]="form.get('clienteId')?.value"
                [hasExistingItems]="hayTrabajoEditado()"
                [warningMessage]="avisoPresupuestoExistenteIa()"
                [focusOnInit]="focusAiOnInit"
                [disabled]="!auth.canMutate()"
                (draftGenerated)="cargarBorradorIa($event)" (stateChange)="onIaPanelState($event)" />
              @if (pendientesIa().precio || pendientesIa().cantidad || pendientesIa().revision) {
                <div class="pending-ai-summary" role="status" aria-live="polite" aria-atomic="true">
                  {{ 'budQuick.ai.pendingSummary' | translate:{price: pendientesIa().precio, quantity: pendientesIa().cantidad, review: pendientesIa().revision} }}
                </div>
              }
              @if (cantidadSegurasPorConfirmar() > 0) {
                <button #safeConfirmTrigger mat-stroked-button type="button" class="confirm-ai-btn confirm-safe-btn"
                  (click)="abrirConfirmacionSeguras()">
                  {{ 'budQuick.ai.confirmSafe' | translate:{count: cantidadSegurasPorConfirmar()} }}
                </button>
              }
              @if (confirmacionSegurasAbierta) {
                <dialog #safeConfirmationDialog class="safe-confirmation-dialog"
                  aria-modal="true" aria-labelledby="safe-confirmation-title"
                  aria-describedby="safe-confirmation-description"
                  tabindex="-1" (cancel)="cancelarConfirmacionSeguras()">
                  <h2 id="safe-confirmation-title">{{ 'budQuick.ai.safeDialogTitle' | translate }}</h2>
                  <p id="safe-confirmation-description">{{ 'budQuick.ai.safeDialogDescription' | translate }}</p>
                  <ul>
                    @for (item of resumenConfirmacionSeguras; track item.lineId) {
                      <li>{{ item.description }} · {{ item.materialName }} · {{ item.price | number:'1.2-2' }} €</li>
                    }
                  </ul>
                  <div class="safe-confirmation-actions">
                    <button mat-stroked-button type="button" class="safe-dialog-button"
                      (click)="cancelarConfirmacionSeguras()">
                      {{ 'budQuick.ai.cancelSafe' | translate }}
                    </button>
                    <button mat-raised-button color="primary" type="button" class="safe-dialog-button"
                      (click)="confirmarTodasSeguras()">
                      {{ 'budQuick.ai.confirmAllSafe' | translate }}
                    </button>
                  </div>
                </dialog>
              }
              <div formArrayName="manualItems">
                @for (item of manualItems.controls; track item; let i = $index) {
                  <div [formGroupName]="i" class="item-row manual-row">
                    @if (showVisibilityColumn) {
                      <mat-checkbox formControlName="visiblePdf" [matTooltip]="'budgetForm.tooltipVisiblePdf' | translate" class="visibility-check"></mat-checkbox>
                    }
                    <mat-form-field appearance="outline" class="desc-wide">
                      <mat-label>{{ 'factForm.description' | translate }}</mat-label>
                      <input matInput formControlName="tareaManual" [placeholder]="'budgetForm.manualDescPh' | translate">
                    </mat-form-field>
                    @if (item.get('iaSugerida')?.value) {
                      <div class="ai-review-info">
                        <app-presupuesto-ia-price-info
                          [priceOrigin]="item.get('precioOrigen')?.value"
                          [approximate]="item.get('precioAproximado')?.value === true"
                          [catalogPrice]="item.get('precioCatalogo')?.value"
                          [includedInPreviousLine]="item.get('precioIncluidoEnLineaAnterior')?.value === true"
                          [currentPrice]="item.get('precioUnitario')?.value" />
                        @if (item.get('faltaPrecio')?.value) {
                          <p class="ia-warning"><mat-icon aria-hidden="true">warning_amber</mat-icon>{{ 'budQuick.ai.missingPrice' | translate }}</p>
                        }
                        @if (item.get('cantidadDudosa')?.value) {
                          <p class="ia-warning"><mat-icon aria-hidden="true">warning_amber</mat-icon>{{ 'budQuick.ai.missingQuantity' | translate }}</p>
                        }
                        @if (!item.get('iaRevisada')?.value) {
                          <button type="button" mat-stroked-button (click)="confirmarSugerenciaIa(i)">
                            <mat-icon>task_alt</mat-icon>{{ 'budQuick.ai.confirmSuggestion' | translate }}
                          </button>
                        }
                      </div>
                    }
                    <mat-form-field appearance="outline" class="qty-with-calc">
                      <mat-label>{{ 'factForm.qty' | translate }}</mat-label>
                      <input matInput type="text" inputmode="decimal" formControlName="cantidad" autocomplete="off" (input)="actualizarCantidadIa(i)">
                      <button
                        matSuffix
                        mat-icon-button
                        type="button"
                        (click)="openCalculadora(manualItems, i)"
                        [matTooltip]="'budgetForm.calcM2Tooltip' | translate"
                        [attr.aria-label]="'budgetForm.calcM2Tooltip' | translate"
                      >
                        <mat-icon>calculate</mat-icon>
                      </button>
                    </mat-form-field>
                    <mat-form-field appearance="outline">
                      <mat-label>{{ 'factForm.unitPrice' | translate }}</mat-label>
                      <input matInput type="text" inputmode="decimal" formControlName="precioUnitario" autocomplete="off" (input)="actualizarPrecioIa(i)">
                    </mat-form-field>
                    <mat-checkbox formControlName="aplicaIva">{{ 'factForm.vatShort' | translate }}</mat-checkbox>
                    <button type="button" mat-icon-button color="warn" (click)="removeManualItem(i)" [matTooltip]="'cliList.tooltipDelete' | translate">
                      <mat-icon>delete</mat-icon>
                    </button>
                  </div>
                }
              </div>
              <button type="button" mat-stroked-button (click)="addManualTaskItem()">
                <mat-icon>add</mat-icon>
                {{ 'budgetForm.addManualTask' | translate }}
              </button>
              <mat-checkbox [(ngModel)]="showVisibilityColumn" [ngModelOptions]="{standalone: true}" class="visibility-toggle">
                {{ 'budgetForm.showVisibilityToggle' | translate }}
              </mat-checkbox>
            </div>

            <!-- Condiciones predefinidas (API) + nota libre; sin variables técnicas en pantalla -->
            <div class="section condiciones-compact">
              <app-condiciones-presupuesto
                [disponibles]="condicionesCatalogo"
                formControlName="condiciones"
              />
            </div>

            @if (mostrarSeccionAnticipo()) {
            @if (estadoMuestraFlujoAnticipo()) {
            <div class="section anticipo-section">
              <h3>{{ 'budgetForm.depositFiscalTitle' | translate }}</h3>
              <p class="hint-anticipo">
                {{ 'budgetForm.depositFiscalHint' | translate }}
              </p>
              @if (!estadoPermiteRegistrarAnticipoApi()) {
                <p class="hint-estado">
                  {{ 'budgetForm.depositStateHintAccepted' | translate }}
                </p>
              }
              @if (!auth.canMutate()) {
                <p class="hint-estado">{{ 'budgetForm.depositReadOnlyHint' | translate }}</p>
              }
              @if (resumenAnticipo) {
                <div class="anticipo-resumen-block">
                  @if (!resumenAnticipo.tieneAnticipoRegistrado) {
                    @if (estadoPermiteRegistrarAnticipoApi()) {
                    <div class="discount-fields">
                      <mat-form-field appearance="outline">
                        <mat-label>{{ 'budgetForm.depositAmountLabel' | translate }}</mat-label>
                        <input matInput type="number" [(ngModel)]="anticipoRegImporte" [ngModelOptions]="{standalone: true}" min="0.01" step="0.01" placeholder="0">
                      </mat-form-field>
                      <mat-form-field appearance="outline">
                        <mat-label>{{ 'budgetForm.depositDateLabel' | translate }}</mat-label>
                        <input matInput type="date" [(ngModel)]="anticipoRegFecha" [ngModelOptions]="{standalone: true}">
                      </mat-form-field>
                      <button type="button" mat-raised-button color="primary" (click)="registrarAnticipoClick()" [disabled]="anticipoCargando || !auth.canMutate()">
                        {{ 'budgetForm.registerDeposit' | translate }}
                      </button>
                    </div>
                    } @else {
                    <p class="text-muted">{{ 'budgetForm.depositChangeStateHint' | translate }}</p>
                    }
                  } @else {
                    <div class="resumen-grid">
                      <span>{{ 'budgetForm.resumenTotal' | translate }}</span><span>{{ resumenAnticipo.totalPresupuesto | number:'1.2-2' }} €</span>
                      <span>{{ 'budgetForm.resumenAnticipo' | translate }}</span><span>{{ resumenAnticipo.importeAnticipo | number:'1.2-2' }} €</span>
                      <span>{{ 'budgetForm.resumenBaseIvaAnticipo' | translate }}</span><span>{{ resumenAnticipo.baseAnticipo | number:'1.2-2' }} € / {{ resumenAnticipo.ivaAnticipo | number:'1.2-2' }} €</span>
                      <span>{{ 'budgetForm.resumenPendiente' | translate }}</span><span>{{ resumenAnticipo.importePendiente | number:'1.2-2' }} €</span>
                    </div>
                    @if (!resumenAnticipo.anticipoYaFacturado) {
                      <button type="button" mat-raised-button color="primary" (click)="generarFacturaAnticipoClick()" [disabled]="anticipoCargando || !auth.canMutate()" class="anticipo-btn">
                        {{ 'budgetForm.genDepositInvoice' | translate }}
                      </button>
                    } @else if (!facturaPrincipalId) {
                      <button type="button" mat-raised-button color="accent" (click)="confirmarFacturaFinal()" [disabled]="anticipoCargando || !auth.canMutate()" class="anticipo-btn">
                        {{ 'budgetForm.genFinalInvoice' | translate }}
                      </button>
                    } @else {
                      <p class="hint-ok">{{ 'budgetForm.finalInvoiceAlready' | translate }}</p>
                    }
                  }
                </div>
              } @else if (anticipoResumenLoading) {
                <p class="text-muted">{{ 'budgetForm.depositLoading' | translate }}</p>
              } @else {
                <p class="text-muted">{{ 'budgetForm.depositLoadFail' | translate }}</p>
              }
            </div>
            } @else {
            <div class="section anticipo-section anticipo-cartel-solo">
              <h3>{{ 'budgetForm.depositCartelOnlyTitle' | translate }}</h3>
              <p class="hint-anticipo">
                {{ 'budgetForm.depositCartelOnlyHint' | translate }}
              </p>
            </div>
            }
            }

            <!-- 4. Descuentos -->
            <div class="section discount-section">
              <h3>{{ 'budgetForm.discountSectionTitle' | translate }}</h3>
              <div class="discount-fields">
                <mat-form-field appearance="outline">
                  <mat-label>{{ 'budgetForm.discountPct' | translate }}</mat-label>
                  <input matInput type="number" formControlName="descuentoGlobalPorcentaje" min="0" max="100" step="0.01">
                </mat-form-field>
                <mat-form-field appearance="outline">
                  <mat-label>{{ 'budgetForm.discountEur' | translate }}</mat-label>
                  <input matInput type="number" formControlName="descuentoGlobalFijo" min="0" step="0.01">
                </mat-form-field>
                <mat-checkbox formControlName="descuentoAntesIva">{{ 'budgetForm.discountBeforeVat' | translate }}</mat-checkbox>
              </div>
            </div>

            <!-- 5. Resumen de costes -->
            <div class="section cost-summary">
              <h3>{{ 'budgetForm.costSummaryTitle' | translate }}</h3>
              <div class="summary-rows">
                <div class="summary-row">
                  <span>{{ 'budgetForm.subtotalItems' | translate }}</span>
                  <span>{{ costesResumen.subtotalItems | number:'1.2-2' }} €</span>
                </div>
                @if (costesResumen.descuentoPorcentaje > 0 || costesResumen.descuentoFijo > 0) {
                  <div class="summary-row discount">
                    <span>{{ 'budgetForm.discountApplied' | translate }}</span>
                    <span>- {{ costesResumen.descuentoTotal | number:'1.2-2' }} €</span>
                  </div>
                }
                <div class="summary-row">
                  <span>{{ 'budgetForm.baseVat' | translate }}</span>
                  <span>{{ costesResumen.baseIva | number:'1.2-2' }} €</span>
                </div>
                @if (form.get('ivaHabilitado')?.value) {
                  <div class="summary-row">
                    <span>{{ 'budgetForm.vat21' | translate }}</span>
                    <span>{{ costesResumen.iva | number:'1.2-2' }} €</span>
                  </div>
                }
                <div class="summary-row total">
                  <span>{{ 'budgetForm.total' | translate }}</span>
                  <span>{{ costesResumen.total | number:'1.2-2' }} €</span>
                </div>
              </div>
            </div>

            <div class="actions">
              <button mat-button type="button" routerLink="/presupuestos">{{ 'common.cancel' | translate }}</button>
              <button
                mat-raised-button
                color="primary"
                type="submit"
                [attr.aria-describedby]="motivosBloqueoCrear().length ? 'budget-submit-block-reasons' : null"
                [disabled]="botonCrearDeshabilitado()"
              >
                {{ isEdit ? ('common.save' | translate) : ('common.create' | translate) }}
              </button>
              @if (motivosBloqueoCrear().length) {
                <ul id="budget-submit-block-reasons" class="submit-block-reasons" role="status" aria-live="polite">
                  @for (reason of motivosBloqueoCrear(); track reason) {
                    <li>{{ reason }}</li>
                  }
                </ul>
              }
            </div>
            @if (isEdit && id) {
              <app-presupuesto-seguimiento
                [presupuestoId]="id"
                [enviadoAt]="presupuestoActual?.enviadoAt"
                [canalEnvio]="presupuestoActual?.canalEnvio"
                [alertCount]="presupuestoActual?.seguimientoAvisosEnviados ?? 0"
                [lastAlertAt]="presupuestoActual?.seguimientoUltimoAvisoAt"
                [silenced]="presupuestoActual?.seguimientoSilenciado ?? false"
                [clientPhone]="presupuestoActual?.clienteTelefono"
                [clientCountry]="presupuestoActual?.clientePais"
                (resend)="abrirPanelEnvio()"
              />
            }
          </form>
          @if (isEdit && presupuestoActual && mostrarPanelReenvio) {
            <app-enviar-presupuesto
              [presupuesto]="presupuestoActual"
              [mostrarOpcionesAlInicio]="true"
              (enviado)="presupuestoActual = $event"
            />
          }
          }
        </mat-card-content>
      </mat-card>
    </div>
  `,
    styles: [`
    .full-width { width: 100%; display: block; margin-bottom: 16px; }
    .form-row { display: flex; flex-wrap: wrap; gap: 16px; align-items: center; margin-bottom: 20px; }
    .form-row mat-form-field { flex: 1; min-width: 200px; }
    .cliente-block { flex-direction: column; align-items: stretch; }
    .cliente-modo { width: 100%; margin-bottom: 8px; }
    .modo-label { display: block; font-size: 12px; color: var(--app-text-secondary, #64748b); margin-bottom: 8px; }
    .modo-radios { display: flex; flex-wrap: wrap; gap: 16px; }
    .opt-line { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
    .badge-fiscal {
      font-size: 11px;
      font-weight: 500;
      color: var(--app-text-secondary, #64748b);
      background: rgba(15, 23, 42, 0.06);
      padding: 2px 8px;
      border-radius: 6px;
    }
    :host-context(html.app-dark-theme) .badge-fiscal {
      background: rgba(148, 163, 184, 0.16);
    }
    .nuevo-rapido { display: flex; flex-wrap: wrap; gap: 12px; align-items: flex-start; width: 100%; }
    .nombre-nuevo { flex: 1; min-width: 200px; }

    .section {
      margin: 24px 0;
      padding: 20px;
      background: var(--app-bg-card, #fff);
      border-radius: 8px;
      border: 1px solid var(--app-border, rgba(15, 23, 42, 0.08));
    }
    .section h3 {
      margin: 0 0 16px 0;
      font-size: 16px;
      font-weight: 600;
      color: var(--app-text-primary, #0f172a);
    }

    /* Tintes suaves (rgba) en lugar de fondos claros fijos — compatibles con modo oscuro */
    .materiales-section {
      background: rgba(30, 58, 138, 0.05);
      border-color: rgba(30, 58, 138, 0.18);
    }
    .tareas-section {
      background: rgba(180, 83, 9, 0.07);
      border-color: rgba(180, 83, 9, 0.22);
    }
    .discount-section {
      background: rgba(15, 23, 42, 0.03);
      border-color: var(--app-border, rgba(15, 23, 42, 0.08));
    }
    .cost-summary {
      background: rgba(46, 125, 50, 0.09);
      border: 1px solid rgba(46, 125, 50, 0.28);
    }
    .condiciones-compact {
      background: rgba(15, 23, 42, 0.025);
      border-color: var(--app-border, rgba(15, 23, 42, 0.08));
      padding-top: 16px;
      padding-bottom: 16px;
    }
    .anticipo-section {
      background: rgba(245, 158, 11, 0.1);
      border-color: rgba(245, 158, 11, 0.35);
    }
    .hint-anticipo {
      font-size: 12px;
      color: #92400e;
      margin: 0 0 12px 0;
    }
    :host-context(html.app-dark-theme) .hint-anticipo {
      color: #fcd34d;
    }
    .anticipo-resumen-block { margin-top: 8px; }
    .resumen-grid {
      display: grid;
      grid-template-columns: 1fr auto;
      gap: 8px 16px;
      font-size: 14px;
      margin-bottom: 16px;
      max-width: 520px;
    }
    .anticipo-btn { margin-top: 8px; }
    .hint-ok { font-size: 13px; color: #2e7d32; margin: 8px 0 0 0; }
    :host-context(html.app-dark-theme) .hint-ok { color: #86efac; }
    .hint-estado {
      font-size: 13px;
      color: var(--app-text-primary, #0f172a);
      background: var(--app-bg-card, #fff);
      padding: 10px 12px;
      border-radius: 8px;
      border: 1px solid rgba(245, 158, 11, 0.45);
      margin-bottom: 12px;
    }
    :host-context(html.app-dark-theme) .hint-estado {
      border-color: rgba(251, 191, 36, 0.35);
    }
    .anticipo-cartel-solo {
      background: rgba(245, 158, 11, 0.1);
      border-color: rgba(245, 158, 11, 0.35);
    }
    .text-muted { color: var(--app-text-muted, rgba(0, 0, 0, 0.45)); }

    :host-context(html.app-dark-theme) .materiales-section {
      background: rgba(96, 165, 250, 0.1);
      border-color: rgba(96, 165, 250, 0.28);
    }
    :host-context(html.app-dark-theme) .tareas-section {
      border-color: rgba(251, 191, 36, 0.28);
      background: rgba(251, 191, 36, 0.1);
    }
    :host-context(html.app-dark-theme) .discount-section {
      background: rgba(255, 255, 255, 0.05);
    }
    :host-context(html.app-dark-theme) .cost-summary {
      background: rgba(129, 199, 132, 0.14);
      border-color: rgba(129, 199, 132, 0.35);
    }
    :host-context(html.app-dark-theme) .condiciones-compact {
      background: rgba(255, 255, 255, 0.04);
    }
    :host-context(html.app-dark-theme) .anticipo-section,
    :host-context(html.app-dark-theme) .anticipo-cartel-solo {
      background: rgba(251, 191, 36, 0.12);
      border-color: rgba(251, 191, 36, 0.35);
    }

    .top-materiales { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-bottom: 12px; }
    .top-label { font-size: 12px; color: var(--app-text-secondary, #64748b); }
    .chip-btn { font-size: 12px; }

    .item-row {
      display: flex; flex-wrap: wrap; gap: 16px; align-items: center;
      margin-bottom: 16px;
      padding: 16px;
      background: var(--app-bg-card, #fff);
      border-radius: 8px;
      border: 1px solid var(--app-border, rgba(15, 23, 42, 0.12));
    }
    :host-context(html.app-dark-theme) .item-row {
      background: rgba(15, 18, 24, 0.85);
      border-color: rgba(255, 255, 255, 0.1);
    }
    .item-row mat-form-field { flex: 1; min-width: 120px; }
    .item-row .material-select { min-width: 200px; }
    .item-row .desc-wide { min-width: 250px; }
    .item-row .visibility-check { margin-right: 8px; }
    .item-row mat-checkbox { margin: 0 8px; }
    .ai-review-info { flex: 1 1 100%; }
    .ia-warning { display: flex; align-items: center; gap: 6px; margin: 4px 0; color: #713f12; font-weight: 600; }
    .ia-status, .ia-pending { margin: 6px 0 12px; font-weight: 600; }
    .pending-ai-summary { margin: 12px 0; padding: 12px; border-radius: 8px; background: rgba(30, 58, 138, .06); }
    .safe-confirmation-dialog { width: min(600px, calc(100vw - 32px)); max-height: min(80vh, 720px); padding: 20px; border: 0; border-radius: 12px; box-shadow: 0 16px 48px rgba(15, 23, 42, .28); }
    .safe-confirmation-dialog::backdrop { background: rgba(15, 23, 42, .55); }
    .safe-confirmation-actions { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 10px; }
    .ia-error { color: #b91c1c; }

    .section > button { margin-top: 8px; margin-right: 16px; }
    .visibility-toggle { margin-top: 12px; display: block; }

    .discount-fields { display: flex; flex-wrap: wrap; gap: 16px; align-items: center; }
    .discount-fields mat-form-field { max-width: 120px; }

    .summary-rows { display: flex; flex-direction: column; gap: 8px; }
    .summary-row {
      display: flex;
      justify-content: space-between;
      font-size: 14px;
      color: var(--app-text-primary, #0f172a);
    }
    .summary-row.discount { color: #2e7d32; }
    :host-context(html.app-dark-theme) .summary-row.discount { color: #86efac; }
    .summary-row.total {
      font-weight: 600;
      font-size: 18px;
      margin-top: 8px;
      padding-top: 8px;
      border-top: 1px solid rgba(46, 125, 50, 0.35);
    }
    :host-context(html.app-dark-theme) .summary-row.total {
      border-top-color: rgba(129, 199, 132, 0.4);
    }

    .actions { display: flex; gap: 16px; margin-top: 24px; }
    .submit-block-reasons { flex-basis: 100%; margin: 0; color: #991b1b; font-weight: 600; }
    .creacion-completada { display: grid; justify-items: start; gap: 12px; padding: 8px 0; }
    .creacion-completada > mat-icon { color: #168447; font-size: 40px; width: 40px; height: 40px; }
    .creacion-completada h2, .creacion-completada p { margin: 0; }
  `]
})
export class PresupuestoFormComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly cdr = inject(ChangeDetectorRef);

  form: FormGroup;
  /** Catálogo de condiciones (solo claves + textos desde API). */
  condicionesCatalogo: PresupuestoCondicionDisponible[] = [];
  clientes: Cliente[] = [];
  materiales: Material[] = [];
  topMateriales: Material[] = [];
  showVisibilityColumn = false;
  isEdit = false;
  id?: number;
  presupuestoActual: import('../../../core/models/presupuesto.model').Presupuesto | null = null;
  creacionCompletada = false;
  mostrarPanelReenvio = false;
  /** existente: desplegable; nuevo: solo nombre y alta rápida. */
  clienteModo: 'existente' | 'nuevo' = 'existente';
  nombreClienteNuevo = '';
  textoObraIa = '';
  iaLoading = false;
  iaStatusMessage = '';
  iaErrorMessage = '';
  saving = false;
  focusAiOnInit = false;
  confirmacionSegurasAbierta = false;
  resumenConfirmacionSeguras: Array<{ lineId: number; description: string; materialName: string; price: number }> = [];
  @ViewChild(PresupuestoIaPanelComponent) private iaPanel?: PresupuestoIaPanelComponent;
  @ViewChild('safeConfirmationDialog') private safeConfirmationDialog?: ElementRef<HTMLDialogElement>;
  @ViewChild('safeConfirmTrigger') private safeConfirmTrigger?: ElementRef<HTMLButtonElement>;
  private nextIaLineId = 1;

  /** Resumen API del flujo de anticipo (solo presupuesto Aceptado en edición). */
  resumenAnticipo: AnticipoResumen | null = null;
  anticipoResumenLoading = false;
  anticipoCargando = false;
  /** Factura de venta principal (NORMAL o FINAL_CON_ANTICIPO), si existe. */
  facturaPrincipalId: number | null = null;
  anticipoRegImporte: number | '' = '';
  anticipoRegFecha = '';

  get materialItems(): FormArray {
    return this.form.get('materialItems') as FormArray;
  }

  abrirPanelEnvio(): void {
    this.mostrarPanelReenvio = true;
  }

  get manualItems(): FormArray {
    return this.form.get('manualItems') as FormArray;
  }

  get costesResumen() {
    return this.calcularCostesResumen();
  }

  botonCrearDeshabilitado(): boolean {
    return this.motivosBloqueoCrear().length > 0;
  }

  motivosBloqueoCrear(): string[] {
    const reasons: string[] = [];
    const translate = (key: string, params?: Record<string, unknown>) =>
      this.translate.instant(`budQuick.ai.submitBlock.${key}`, params);
    if (!this.auth.canMutate()) reasons.push(translate('permission'));
    if (this.saving) reasons.push(translate('saving'));
    if (this.iaLoading) reasons.push(translate('generating'));
    if (this.form.pending) reasons.push(translate('validationPending'));
    if (!this.form.get('clienteId')?.value) reasons.push(translate('customer'));
    const pending = this.pendientesIa();
    if (pending.revision) reasons.push(translate('review', { count: pending.revision }));
    let activeLines = 0;
    let lineNumber = 0;
    for (const { ctrl } of this.getAllItems()) {
      const values = ctrl.getRawValue();
      const active = Boolean(values.materialId) || String(values.tareaManual ?? '').trim().length > 0;
      if (!active) continue;
      activeLines++;
      lineNumber++;
      const quantity = parsePresupuestoDecimal(values.cantidad);
      const price = parsePresupuestoDecimal(values.precioUnitario);
      if (values.cantidadDudosa === true || quantity == null || quantity < 0.001) {
        reasons.push(translate('invalidQuantity', { line: lineNumber }));
      }
      if (values.faltaPrecio === true || price == null || price < 0 ||
          (values.iaSugerida === true && price <= 0)) {
        reasons.push(translate('invalidPrice', { line: lineNumber }));
      }
    }
    if (!activeLines) reasons.push(translate('line'));
    if (this.form.invalid && reasons.length === 0) reasons.push(translate('invalidForm'));
    return [...new Set(reasons)];
  }

  private getIaRevisionDatos(): PresupuestoIaRevisionDatos[] {
    return this.manualItems.controls.filter((item) => item.get('iaSugerida')?.value === true)
      .map((item) => {
        const values = item.getRawValue();
        return {
          iaSugerida: values.iaSugerida === true,
          iaRevisada: values.iaRevisada === true,
          confianza: values.confianza,
          materialId: values.materialId,
          materialEnCatalogo: this.materiales.some((material) => material.id === values.materialId),
          faltaPrecio: values.faltaPrecio === true,
          cantidadDudosa: values.cantidadDudosa === true,
          cantidad: values.cantidad,
          precioUnitario: values.precioUnitario,
        };
      });
  }

  pendientesIa(): { precio: number; cantidad: number; revision: number } {
    return evaluarPendientesPresupuestoIa(this.getIaRevisionDatos());
  }

  cantidadSegurasPorConfirmar(): number {
    return this.getIaRevisionDatos().filter(esSugerenciaPresupuestoIaSegura).length;
  }

  private esSeguraParaConfirmacion(control: AbstractControl): boolean {
    const values = control.getRawValue();
    return esSugerenciaPresupuestoIaSegura({
      iaSugerida: values.iaSugerida === true,
      iaRevisada: values.iaRevisada === true,
      confianza: values.confianza,
      materialId: values.materialId,
      materialEnCatalogo: this.materiales.some((material) => material.id === values.materialId),
      faltaPrecio: values.faltaPrecio === true,
      cantidadDudosa: values.cantidadDudosa === true,
      cantidad: values.cantidad,
      precioUnitario: values.precioUnitario,
    });
  }

  abrirConfirmacionSeguras(): void {
    this.resumenConfirmacionSeguras = this.manualItems.controls
      .filter((control) => this.esSeguraParaConfirmacion(control))
      .map((control) => {
        const values = control.getRawValue();
        const material = this.materiales.find((candidate) => candidate.id === values.materialId);
        return {
          lineId: values.iaLineId as number,
          description: String(values.tareaManual ?? ''),
          materialName: String(values.materialNombre || material?.nombre || ''),
          price: parsePresupuestoDecimal(values.precioUnitario) ?? 0,
        };
      });
    if (!this.resumenConfirmacionSeguras.length) return;
    this.confirmacionSegurasAbierta = true;
    setTimeout(() => {
      const dialog = this.safeConfirmationDialog?.nativeElement;
      if (!dialog) return;
      if (!dialog.open) {
        if (typeof dialog.showModal === 'function') dialog.showModal();
        else dialog.setAttribute('open', '');
      }
      dialog.focus();
    }, 0);
  }

  confirmarTodasSeguras(): void {
    for (const summary of this.resumenConfirmacionSeguras) {
      const control = this.manualItems.controls
        .find((candidate) => candidate.get('iaLineId')?.value === summary.lineId);
      if (control && this.esSeguraParaConfirmacion(control)) control.get('iaRevisada')?.setValue(true);
    }
    this.cerrarDialogoSeguras();
  }

  cancelarConfirmacionSeguras(): void {
    this.cerrarDialogoSeguras();
  }

  private cerrarDialogoSeguras(): void {
    const dialog = this.safeConfirmationDialog?.nativeElement;
    if (dialog?.open) {
      if (typeof dialog.close === 'function') dialog.close();
      else dialog.removeAttribute('open');
    }
    this.confirmacionSegurasAbierta = false;
    this.resumenConfirmacionSeguras = [];
    setTimeout(() => this.safeConfirmTrigger?.nativeElement?.focus(), 0);
  }

  generarConIa(): void {
    const text = this.textoObraIa.trim();
    if (!text || text.length > 8000 || this.iaLoading || !this.auth.canMutate()) return;
    const clienteId = this.form.get('clienteId')?.value as number | null;
    this.iaPanel?.generarBorrador(text, clienteId, this.hayTrabajoEditado());
  }

  avisoPresupuestoExistenteIa(): string {
    if (!this.isEdit) return '';
    if (this.presupuestoActual?.enviadoAt) {
      return this.translate.instant('budQuick.ai.sentWarning');
    }
    if ((this.form.get('estado')?.value ?? 'Pendiente').toString().trim() !== 'Pendiente') {
      return this.translate.instant('budQuick.ai.nonPendingWarning');
    }
    return '';
  }

  onIaPanelState(state: PresupuestoIaPanelState): void {
    this.iaLoading = state.loading;
    this.iaStatusMessage = state.statusMessage;
    this.iaErrorMessage = state.errorMessage;
  }

  hayTrabajoEditado(): boolean {
    return this.manualItems.controls.some((item) =>
      String(item.get('tareaManual')?.value ?? '').trim()) ||
      this.materialItems.controls.some((item) => item.get('materialId')?.value != null);
  }

  cargarBorradorIa(draft: PresupuestoIaBorradorResponse): void {
    this.materialItems.clear();
    this.manualItems.clear();
    for (const item of draft.items) {
      this.manualItems.push(this.createItemGroup({
        iaLineId: this.nextIaLineId++,
        materialId: item.materialId,
        tareaManual: item.tareaManual,
        cantidad: item.cantidadDudosa ? null : item.cantidad,
        precioUnitario: item.precioUnitario,
        aplicaIva: item.aplicaIva ?? true,
        descuentoPorcentaje: item.descuentoPorcentaje ?? 0,
        descuentoFijo: item.descuentoFijo ?? 0,
        visiblePdf: item.visiblePdf ?? true,
        isManualTask: true,
        iaSugerida: true,
        iaRevisada: false,
        confianza: item.confianza,
        faltaPrecio: item.faltaPrecio || item.precioUnitario <= 0,
        cantidadDudosa: item.cantidadDudosa || item.cantidad == null || item.cantidad <= 0,
        precioOrigen: item.precioOrigen ?? (item.precioUnitario > 0 ? 'catalogo' : 'ninguno'),
        precioAproximado: item.precioAproximado === true,
        precioCatalogo: item.precioCatalogo ?? null,
        precioIncluidoEnLineaAnterior: item.precioIncluidoEnLineaAnterior === true,
      }));
    }
    if (draft.notaAdicional) {
      const conditions = this.form.get('condiciones')?.value as CondicionesPresupuestoFormValue;
      this.form.patchValue({ condiciones: { ...conditions, notaAdicional: draft.notaAdicional } });
    }
    this.iaLoading = false;
  }

  confirmarSugerenciaIa(index: number): void {
    const item = this.manualItems.at(index);
    if (item.get('iaSugerida')?.value === true) item.get('iaRevisada')?.setValue(true);
  }

  actualizarPrecioIa(index: number): void {
    const item = this.manualItems.at(index);
    if (item.get('iaSugerida')?.value !== true) return;
    const price = parsePresupuestoDecimal(item.get('precioUnitario')?.value);
    item.patchValue({
      faltaPrecio: price == null || price <= 0,
      precioOrigen: 'ninguno',
      precioAproximado: false,
    }, { emitEvent: false });
  }

  actualizarCantidadIa(index: number): void {
    const item = this.manualItems.at(index);
    if (item.get('iaSugerida')?.value !== true) return;
    const quantity = parsePresupuestoDecimal(item.get('cantidad')?.value);
    item.patchValue({ cantidadDudosa: quantity == null || quantity <= 0 }, { emitEvent: false });
  }

  /**
   * Muestra el bloque de anticipo en edición.
   * No usar `auth.canMutate()` aquí: los signals del servicio inyectado no siempre disparan
   * el mismo ciclo de detección de cambios que el componente; además `roleMutateGuard` ya exige escritura en esta ruta.
   */
  mostrarSeccionAnticipo(): boolean {
    return this.isEdit === true && this.id != null && this.id > 0 && !Number.isNaN(this.id);
  }

  /** Muestra el bloque con resumen y acciones (no el cartel corto de Pendiente/Rechazado). */
  estadoMuestraFlujoAnticipo(): boolean {
    const e = (this.form.get('estado')?.value ?? '').toString().trim();
    return e === 'Aceptado' || e === 'En ejecución';
  }

  /** La API solo permite registrar anticipo con presupuesto en Aceptado. */
  estadoPermiteRegistrarAnticipoApi(): boolean {
    return (this.form.get('estado')?.value ?? '').toString().trim() === 'Aceptado';
  }

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    public auth: AuthService,
    private presupuestoService: PresupuestoService,
    private clienteService: ClienteService,
    private materialService: MaterialService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private translate: TranslateService
  ) {
    this.form = this.fb.group({
      clienteId: [null, Validators.required],
      estado: ['Pendiente'],
      ivaHabilitado: [true],
      descuentoGlobalPorcentaje: [0],
      descuentoGlobalFijo: [0],
      descuentoAntesIva: [true],
      condiciones: this.fb.control<CondicionesPresupuestoFormValue>({
        condicionesActivas: [],
        notaAdicional: '',
      }),
      materialItems: this.fb.array([]),
      manualItems: this.fb.array([]),
    });
  }

  ngOnInit(): void {
    this.translate.onLangChange.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.cdr.markForCheck());

    const close = () => this.translate.instant('common.close');
    const showError = (msg: string) => this.snackBar.open(msg, close(), { duration: 5000 });
    this.clienteService.getAll().subscribe({
      next: (data) => (this.clientes = data),
      error: () => showError(this.translate.instant('snack.clientsLoadFail')),
    });
    this.materialService.getAll().subscribe({
      next: (data) => (this.materiales = data),
      error: () => showError(this.translate.instant('snack.materialsLoadFail')),
    });
    this.materialService.getTopUsados().subscribe({
      next: (data) => (this.topMateriales = data),
      error: () => {}, // Opcional: top materiales
    });
    const id = this.route.snapshot.paramMap.get('id');
    this.focusAiOnInit = this.route.snapshot.queryParamMap.get('ia') === '1';
    if (id && id !== 'nuevo') {
      this.isEdit = true;
      this.id = +id;
      forkJoin({
        p: this.presupuestoService.getById(this.id),
        disp: this.presupuestoService.getCondicionesDisponibles(),
      }).subscribe({
        next: ({ p, disp }) => {
          this.presupuestoActual = p;
          this.condicionesCatalogo = disp;
          this.form.patchValue({
            clienteId: p.clienteId,
            estado: p.estado,
            ivaHabilitado: p.ivaHabilitado,
            descuentoGlobalPorcentaje: p.descuentoGlobalPorcentaje ?? 0,
            descuentoGlobalFijo: p.descuentoGlobalFijo ?? 0,
            descuentoAntesIva: p.descuentoAntesIva ?? true,
            condiciones: {
              condicionesActivas: p.condicionesActivas ?? [],
              notaAdicional: p.notaAdicional ?? '',
            },
          });
          this.facturaPrincipalId = p.facturaId ?? null;
          if (this.estadoStrFlujoAnticipo(p.estado)) {
            this.anticipoRegFecha = new Date().toISOString().slice(0, 10);
            this.loadResumenAnticipo();
          }
          this.materialItems.clear();
          this.manualItems.clear();
          p.items.forEach((it) => {
            const group = this.createItemGroup({
              materialId: it.materialId ?? null,
              tareaManual: it.descripcion || '',
              cantidad: it.cantidad,
              precioUnitario: it.precioUnitario,
              aplicaIva: true,
              descuentoPorcentaje: 0,
              descuentoFijo: 0,
              visiblePdf: it.visiblePdf ?? true,
              isManualTask: it.esTareaManual ?? false,
            });
            if (it.esTareaManual) {
              this.manualItems.push(group);
            } else {
              this.materialItems.push(group);
            }
          });
        },
        error: () => this.router.navigate(['/presupuestos']),
      });
    } else {
      this.isEdit = false;
      forkJoin({
        disp: this.presupuestoService.getCondicionesDisponibles(),
        def: this.presupuestoService.getMisCondicionesPredeterminadas(),
      }).subscribe({
        next: ({ disp, def }) => {
          this.condicionesCatalogo = disp;
          this.form.patchValue({
            condiciones: {
              condicionesActivas: def ?? [],
              notaAdicional: '',
            },
          });
        },
        error: () => showError(this.translate.instant('snack.budgetCondicionesLoadFail')),
      });
      const preCliente = this.route.snapshot.queryParamMap.get('clienteId');
      if (preCliente) {
        const n = +preCliente;
        if (!isNaN(n)) {
          this.form.patchValue({ clienteId: n });
        }
      }
      this.addMaterialItem();
    }

    this.form
      .get('estado')
      ?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((est) => {
        if (!this.isEdit || !this.id) return;
        if (this.estadoStrFlujoAnticipo(est)) {
          if (!this.anticipoRegFecha) {
            this.anticipoRegFecha = new Date().toISOString().slice(0, 10);
          }
          this.loadResumenAnticipo();
        } else {
          this.resumenAnticipo = null;
        }
      });
  }

  private estadoStrFlujoAnticipo(estado: string | null | undefined): boolean {
    const e = (estado ?? '').toString().trim();
    return e === 'Aceptado' || e === 'En ejecución';
  }

  private loadResumenAnticipo(): void {
    if (!this.id || !this.estadoMuestraFlujoAnticipo()) return;
    this.anticipoResumenLoading = true;
    this.resumenAnticipo = null;
    this.presupuestoService.getResumenAnticipo(this.id).subscribe({
      next: (r) => {
        this.resumenAnticipo = r;
        this.anticipoResumenLoading = false;
      },
      error: () => {
        this.resumenAnticipo = null;
        this.anticipoResumenLoading = false;
        this.snackBar.open(this.translate.instant('snack.depositSummaryFail'), this.translate.instant('common.close'), {
          duration: 4000,
        });
      },
    });
  }

  registrarAnticipoClick(): void {
    if (!this.id) return;
    const imp = this.anticipoRegImporte === '' ? NaN : +this.anticipoRegImporte;
    if (!this.anticipoRegFecha || isNaN(imp) || imp <= 0) {
      this.snackBar.open(this.translate.instant('snack.depositInvalid'), this.translate.instant('common.close'), {
        duration: 4000,
      });
      return;
    }
    this.anticipoCargando = true;
    this.presupuestoService
      .registrarAnticipo(this.id, { importeAnticipo: imp, fechaAnticipo: this.anticipoRegFecha })
      .subscribe({
        next: (p) => {
          this.facturaPrincipalId = p.facturaId ?? null;
          this.anticipoCargando = false;
          this.snackBar.open(this.translate.instant('snack.depositRegistered'), this.translate.instant('common.close'), {
            duration: 3000,
          });
          this.loadResumenAnticipo();
        },
        error: (err) => {
          this.anticipoCargando = false;
          const raw = err.error?.message || err.error?.detail;
          const msg =
            typeof raw === 'string' && raw.trim() !== ''
              ? raw.trim()
              : this.translate.instant('snack.depositRegisterFail');
          this.snackBar.open(msg, this.translate.instant('common.close'), { duration: 5000 });
        },
      });
  }

  generarFacturaAnticipoClick(): void {
    if (!this.id) return;
    this.anticipoCargando = true;
    this.presupuestoService.generarFacturaAnticipo(this.id).subscribe({
      next: () => {
        this.anticipoCargando = false;
        this.snackBar.open(this.translate.instant('snack.depositInvoiceCreated'), this.translate.instant('common.close'), {
          duration: 3500,
        });
        this.loadResumenAnticipo();
      },
      error: (err) => {
        this.anticipoCargando = false;
        const raw = err.error?.message || err.error?.detail;
        const msg =
          typeof raw === 'string' && raw.trim() !== ''
            ? raw.trim()
            : this.translate.instant('snack.depositInvoiceFail');
        this.snackBar.open(msg, this.translate.instant('common.close'), { duration: 6000 });
      },
    });
  }

  confirmarFacturaFinal(): void {
    const r = this.resumenAnticipo;
    if (!this.id || !r) return;
    const msg = [
      this.translate.instant('budgetForm.finalConfirmLine1', { total: r.totalPresupuesto.toFixed(2) }),
      this.translate.instant('budgetForm.finalConfirmLine2', { pending: r.importePendiente.toFixed(2) }),
      '',
      this.translate.instant('budgetForm.finalConfirmQuestion'),
    ].join('\n');
    this.dialog
      .open(ConfirmDialogComponent, {
        width: '440px',
        data: {
          title: this.translate.instant('budgetForm.finalConfirmTitle'),
          message: msg,
          confirmLabel: this.translate.instant('budgetForm.finalConfirmBtn'),
          confirmColor: 'primary',
        },
      })
      .afterClosed()
      .subscribe((ok) => {
        if (!ok || !this.id) return;
        this.anticipoCargando = true;
        this.presupuestoService.createFacturaFinalFromPresupuesto(this.id).subscribe({
          next: (f) => {
            this.anticipoCargando = false;
            this.facturaPrincipalId = f.id;
            this.snackBar.open(this.translate.instant('snack.finalInvoiceCreated'), this.translate.instant('common.close'), {
              duration: 3000,
            });
            this.router.navigate(['/facturas', f.id]);
          },
          error: (err) => {
            this.anticipoCargando = false;
            const raw = err.error?.message || err.error?.detail;
            const msg =
              typeof raw === 'string' && raw.trim() !== ''
                ? raw.trim()
                : this.translate.instant('snack.finalInvoiceFail');
            this.snackBar.open(msg, this.translate.instant('common.close'), { duration: 6000 });
          },
        });
      });
  }

  /** Texto del campo cerrado: solo nombre (el badge solo se muestra en la lista). */
  nombreClienteSeleccionado(): string {
    const id = this.form.get('clienteId')?.value as number | null | undefined;
    if (id == null) return '';
    return this.clientes.find((c) => c.id === id)?.nombre ?? '';
  }

  onClienteModoChange(m: 'existente' | 'nuevo'): void {
    if (m === 'nuevo') {
      this.form.patchValue({ clienteId: null });
    }
  }

  crearClienteRapido(): void {
    const n = this.nombreClienteNuevo?.trim();
    if (!n) return;
    this.clienteService.createProvisional({ nombre: n }).subscribe({
      next: (c) => {
        this.clientes = [...this.clientes, c].sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'));
        this.form.patchValue({ clienteId: c.id });
        this.snackBar.open(this.translate.instant('snack.inlineClientOk'), this.translate.instant('common.close'), {
          duration: 3500,
        });
        this.clienteModo = 'existente';
        this.nombreClienteNuevo = '';
      },
      error: (err) => {
        const raw = err.error?.message || err.error?.detail;
        const msg =
          typeof raw === 'string' && raw.trim() !== ''
            ? raw.trim()
            : this.translate.instant('snack.inlineClientFail');
        this.snackBar.open(msg, this.translate.instant('common.close'), { duration: 5000 });
      },
    });
  }

  private createItemGroup(values: {
    iaLineId?: number;
    materialId: number | null;
    tareaManual: string;
    cantidad: number | null;
    precioUnitario: number;
    aplicaIva: boolean;
    descuentoPorcentaje: number;
    descuentoFijo: number;
    visiblePdf: boolean;
    isManualTask: boolean;
    iaSugerida?: boolean;
    iaRevisada?: boolean;
    confianza?: string;
    faltaPrecio?: boolean;
    cantidadDudosa?: boolean;
    precioOrigen?: string;
    precioAproximado?: boolean;
    precioCatalogo?: number | null;
    precioIncluidoEnLineaAnterior?: boolean;
  }): FormGroup {
    return this.fb.group({
      iaLineId: [values.iaLineId ?? null],
      materialId: [values.materialId],
      // Sin required aquí: una fila vacía no debe bloquear el botón Crear; onSubmit exige líneas con contenido.
      tareaManual: [values.tareaManual],
      cantidad: [values.cantidad, [Validators.required, Validators.min(0.001)]],
      precioUnitario: [values.precioUnitario, [Validators.required, Validators.min(0)]],
      aplicaIva: [values.aplicaIva],
      descuentoPorcentaje: [values.descuentoPorcentaje],
      descuentoFijo: [values.descuentoFijo],
      visiblePdf: [values.visiblePdf],
      isManualTask: [values.isManualTask],
      iaSugerida: [values.iaSugerida ?? false],
      iaRevisada: [values.iaRevisada ?? false],
      confianza: [values.confianza ?? 'alta'],
      faltaPrecio: [values.faltaPrecio ?? false],
      cantidadDudosa: [values.cantidadDudosa ?? false],
      precioOrigen: [values.precioOrigen ?? 'ninguno'],
      precioAproximado: [values.precioAproximado ?? false],
      precioCatalogo: [values.precioCatalogo ?? null],
      precioIncluidoEnLineaAnterior: [values.precioIncluidoEnLineaAnterior ?? false],
    });
  }

  addMaterialItem(): void {
    this.materialItems.push(
      this.createItemGroup({
        materialId: null,
        tareaManual: '',
        cantidad: 1,
        precioUnitario: 0,
        aplicaIva: true,
        descuentoPorcentaje: 0,
        descuentoFijo: 0,
        visiblePdf: true,
        isManualTask: false,
      })
    );
  }

  addManualTaskItem(): void {
    this.manualItems.push(
      this.createItemGroup({
        materialId: null,
        tareaManual: '',
        cantidad: 1,
        precioUnitario: 0,
        aplicaIva: true,
        descuentoPorcentaje: 0,
        descuentoFijo: 0,
        visiblePdf: true,
        isManualTask: true,
      })
    );
  }

  removeMaterialItem(index: number): void {
    this.materialItems.removeAt(index);
  }

  removeManualItem(index: number): void {
    this.manualItems.removeAt(index);
  }

  openCalculadora(items: FormArray, index: number): void {
    const dialogRef = this.dialog.open(CalculadoraM2Component, {
      width: '520px',
      disableClose: false,
      autoFocus: true,
    });
    dialogRef.afterClosed().subscribe((result: CalculadoraResult | null) => {
      if (!result) return;
      const row = items.at(index) as FormGroup;
      row.patchValue({ cantidad: result.area });
      if (result.incluirDetalle && result.descripcion) {
        row.patchValue({ tareaManual: result.descripcion });
        return;
      }
      const desc = String(row.get('tareaManual')?.value ?? '').trim();
      if (!desc && result.descripcion) {
        row.patchValue({ tareaManual: result.descripcion });
      }
    });
  }

  addMaterialFromTop(material: Material): void {
    const mid = material.id;
    for (let i = 0; i < this.materialItems.length; i++) {
      const g = this.materialItems.at(i) as FormGroup;
      if (g.get('materialId')?.value === mid) {
        const cur = +(g.get('cantidad')?.value ?? 0);
        g.patchValue({ cantidad: cur + 1 });
        return;
      }
    }
    this.materialItems.push(
      this.createItemGroup({
        materialId: material.id,
        tareaManual: material.nombre,
        cantidad: 1,
        precioUnitario: material.precioUnitario,
        aplicaIva: true,
        descuentoPorcentaje: 0,
        descuentoFijo: 0,
        visiblePdf: true,
        isManualTask: false,
      })
    );
    const idx = this.materialItems.length - 1;
    this.onMaterialSelect(idx, material.id);
  }

  /**
   * Al elegir un material en el desplegable, si ya existe otra línea con el mismo material,
   * se unen en una sola fila sumando cantidades (mismo criterio que «Más usados»).
   */
  onMaterialSelect(index: number, materialId: number | null): void {
    if (materialId == null) return;
    const material = this.materiales.find((m) => m.id === materialId);
    if (!material) return;

    const indices: number[] = [];
    for (let i = 0; i < this.materialItems.length; i++) {
      if (this.materialItems.at(i).get('materialId')?.value === materialId) {
        indices.push(i);
      }
    }
    if (indices.length <= 1) {
      this.materialItems.at(index).patchValue({
        tareaManual: material.nombre,
        precioUnitario: material.precioUnitario,
      });
      return;
    }

    const keep = Math.min(...indices);
    let totalCantidad = 0;
    for (const i of indices) {
      totalCantidad += +(this.materialItems.at(i).get('cantidad')?.value ?? 0);
    }
    (this.materialItems.at(keep) as FormGroup).patchValue({
      materialId,
      tareaManual: material.nombre,
      precioUnitario: material.precioUnitario,
      cantidad: totalCantidad,
    });
    for (const i of indices
      .filter((idx) => idx !== keep)
      .sort((a, b) => b - a)) {
      this.materialItems.removeAt(i);
    }
  }

  private getAllItems(): { ctrl: AbstractControl; isManual: boolean }[] {
    const result: { ctrl: AbstractControl; isManual: boolean }[] = [];
    this.materialItems.controls.forEach((c) => result.push({ ctrl: c, isManual: false }));
    this.manualItems.controls.forEach((c) => result.push({ ctrl: c, isManual: true }));
    return result;
  }

  private calcularCostesResumen(): ReturnType<typeof calcularPresupuestoCostes> {
    const allItems = this.getAllItems();
    return calcularPresupuestoCostes({
      items: allItems.map(({ ctrl }) => {
        const v = ctrl.value;
        return {
          cantidad: parsePresupuestoDecimal(v.cantidad) ?? 0,
          precioUnitario: parsePresupuestoDecimal(v.precioUnitario) ?? 0,
          descuentoPorcentaje: +(v.descuentoPorcentaje ?? 0),
          descuentoFijo: +(v.descuentoFijo ?? 0),
          aplicaIva: v.aplicaIva,
        };
      }),
      descuentoGlobalPorcentaje: +(this.form.get('descuentoGlobalPorcentaje')?.value ?? 0),
      descuentoGlobalFijo: +(this.form.get('descuentoGlobalFijo')?.value ?? 0),
      descuentoAntesIva: this.form.get('descuentoAntesIva')?.value !== false,
      ivaHabilitado: this.form.get('ivaHabilitado')?.value !== false,
    });
  }

  onSubmit(): void {
    if (this.botonCrearDeshabilitado()) return;
    const pending = this.pendientesIa();
    if (pending.precio > 0 || pending.cantidad > 0 || pending.revision > 0) {
      this.form.markAllAsTouched();
      this.snackBar.open(this.translate.instant('budQuick.aiPendingBlock'),
        this.translate.instant('common.close'), { duration: 5000 });
      return;
    }
    const allItems = this.getAllItems();
    const validItems = allItems.filter(({ ctrl }) => {
      const v = ctrl.value;
      const hasMaterial = v.materialId != null;
      const hasDesc = v.tareaManual != null && String(v.tareaManual).trim().length > 0;
      return hasMaterial || hasDesc;
    });
    if (!this.form.get('clienteId')?.value) {
      this.form.markAllAsTouched();
      this.snackBar.open(this.translate.instant('snack.budgetNeedClient'), this.translate.instant('common.close'), {
        duration: 4500,
      });
      return;
    }
    if (validItems.length === 0) {
      this.form.markAllAsTouched();
      this.snackBar.open(this.translate.instant('snack.budgetNeedLine'), this.translate.instant('common.close'), {
        duration: 4500,
      });
      return;
    }
    const items: PresupuestoItemRequest[] = validItems.map(({ ctrl }) => {
      const it = ctrl.value;
      return {
        materialId: it.materialId || undefined,
        tareaManual: it.tareaManual?.trim() || undefined,
        cantidad: parsePresupuestoDecimal(it.cantidad) ?? 0,
        precioUnitario: parsePresupuestoDecimal(it.precioUnitario) ?? 0,
        aplicaIva: it.aplicaIva,
        descuentoPorcentaje: it.descuentoPorcentaje ?? 0,
        descuentoFijo: it.descuentoFijo ?? 0,
        visiblePdf: it.visiblePdf ?? true,
      };
    });
    const value = this.form.value;
    const cond = value.condiciones as CondicionesPresupuestoFormValue | undefined;
    const payload = {
      clienteId: value.clienteId,
      items,
      ivaHabilitado: value.ivaHabilitado,
      estado: value.estado,
      descuentoGlobalPorcentaje: +(value.descuentoGlobalPorcentaje ?? 0),
      descuentoGlobalFijo: +(value.descuentoGlobalFijo ?? 0),
      descuentoAntesIva: value.descuentoAntesIva !== false,
      condicionesActivas: cond?.condicionesActivas ?? [],
      notaAdicional: cond?.notaAdicional?.trim() ? cond.notaAdicional.trim() : undefined,
    };
    const req = this.isEdit && this.id
      ? this.presupuestoService.update(this.id, payload)
      : this.presupuestoService.create(payload);
    this.saving = true;
    req.subscribe({
      next: (presupuesto) => {
        this.saving = false;
        this.presupuestoActual = presupuesto;
        this.creacionCompletada = true;
        this.snackBar.open(
          this.translate.instant(this.isEdit ? 'snack.budgetUpdated' : 'snack.budgetSavedCreated'),
          this.translate.instant('common.close'),
          { duration: 3000 },
        );
      },
      error: (err) => {
        this.saving = false;
        const raw = err.error?.message || err.error?.error;
        const msg =
          typeof raw === 'string' && String(raw).trim() !== ''
            ? String(raw).trim()
            : this.translate.instant('snack.budgetSaveFail');
        this.snackBar.open(msg, this.translate.instant('common.close'), { duration: 5000 });
      },
    });
  }
}
