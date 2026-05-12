import { Component, DestroyRef, OnInit, inject } from '@angular/core';
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
import { TranslateService } from '@ngx-translate/core';

const IVA_RATE = 0.21;

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
    ],
    template: `
    <div class="presupuesto-form">
      <mat-card>
        <mat-card-header>
          <mat-card-title>{{ isEdit ? 'Editar presupuesto' : 'Nuevo presupuesto' }}</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          @if (!isEdit) {
            <div class="section anticipo-section anticipo-cartel-solo">
              <h3>Anticipo / seña</h3>
              <p class="hint-anticipo">
                Tras <strong>guardar</strong> el presupuesto, vuelve a abrirlo con estado <strong>Aceptado</strong> o <strong>En ejecución</strong>
                para registrar el anticipo cobrado y emitir las facturas (anticipo y final).
              </p>
            </div>
          }
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <!-- 1. Cliente y Estado -->
            <div class="form-row cliente-block">
              <div class="cliente-modo">
                <mat-label class="modo-label">Cliente del presupuesto</mat-label>
                <mat-radio-group
                  [(ngModel)]="clienteModo"
                  [ngModelOptions]="{standalone: true}"
                  (ngModelChange)="onClienteModoChange($event)"
                  class="modo-radios"
                >
                  <mat-radio-button value="existente">Seleccionar cliente existente</mat-radio-button>
                  <mat-radio-button value="nuevo">Cliente nuevo rápido</mat-radio-button>
                </mat-radio-group>
              </div>
              @if (clienteModo === 'existente') {
                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>Cliente</mat-label>
                  <mat-select formControlName="clienteId" required>
                    <mat-select-trigger>
                      {{ nombreClienteSeleccionado() }}
                    </mat-select-trigger>
                    @for (c of clientes; track c.id) {
                      <mat-option [value]="c.id">
                        <span class="opt-line">
                          <span>{{ c.nombre }}</span>
                          @if (c.estadoCliente === 'PROVISIONAL') {
                            <span class="badge-fiscal">Sin datos fiscales</span>
                          }
                        </span>
                      </mat-option>
                    }
                  </mat-select>
                  <mat-error>Selecciona un cliente</mat-error>
                </mat-form-field>
              } @else {
                <div class="nuevo-rapido">
                  <mat-form-field appearance="outline" class="nombre-nuevo">
                    <mat-label>Nombre del cliente</mat-label>
                    <input matInput [(ngModel)]="nombreClienteNuevo" [ngModelOptions]="{standalone: true}" placeholder="Ej. Juan García" />
                  </mat-form-field>
                  <button type="button" mat-stroked-button color="primary" (click)="crearClienteRapido()"
                    [disabled]="!auth.canMutate() || !nombreClienteNuevo.trim()">
                    Crear y continuar
                  </button>
                </div>
              }
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Estado</mat-label>
                <mat-select formControlName="estado">
                  <mat-option value="Pendiente">Pendiente</mat-option>
                  <mat-option value="Aceptado">Aceptado</mat-option>
                  <mat-option value="En ejecución">En ejecución</mat-option>
                  <mat-option value="Rechazado">Rechazado</mat-option>
                </mat-select>
              </mat-form-field>
              <mat-checkbox formControlName="ivaHabilitado">IVA incluido</mat-checkbox>
            </div>

            <!-- 2. Materiales (primero, con línea visible al abrir) -->
            <div class="section materiales-section">
              <h3>Materiales</h3>
              @if (topMateriales.length > 0) {
                <div class="top-materiales">
                  <span class="top-label">Más usados:</span>
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
                      <mat-checkbox formControlName="visiblePdf" matTooltip="Visible en el PDF" class="visibility-check"></mat-checkbox>
                    }
                    <mat-form-field appearance="outline" class="material-select">
                      <mat-label>Material</mat-label>
                      <mat-select formControlName="materialId" (selectionChange)="onMaterialSelect(i, $event.value)">
                        <mat-option [value]="null">Seleccionar...</mat-option>
                        @for (m of materiales; track m.id) {
                          <mat-option [value]="m.id">{{ m.nombre }} ({{ m.precioUnitario | number:'1.2-2' }} €)</mat-option>
                        }
                      </mat-select>
                    </mat-form-field>
                    <mat-form-field appearance="outline">
                      <mat-label>Descripción</mat-label>
                      <input matInput formControlName="tareaManual" placeholder="Se rellena al seleccionar material">
                    </mat-form-field>
                    <mat-form-field appearance="outline">
                      <mat-label>Cantidad</mat-label>
                      <input matInput type="number" formControlName="cantidad" min="0.001" step="0.01">
                    </mat-form-field>
                    <mat-form-field appearance="outline">
                      <mat-label>Precio unit.</mat-label>
                      <input matInput type="number" formControlName="precioUnitario" min="0" step="0.01">
                    </mat-form-field>
                    <mat-checkbox formControlName="aplicaIva">IVA</mat-checkbox>
                    <button type="button" mat-icon-button color="warn" (click)="removeMaterialItem(i)" matTooltip="Eliminar">
                      <mat-icon>delete</mat-icon>
                    </button>
                  </div>
                }
              </div>
              <button type="button" mat-stroked-button (click)="addMaterialItem()">
                <mat-icon>add</mat-icon>
                Añadir material
              </button>
            </div>

            <!-- 3. Tareas manuales -->
            <div class="section tareas-section">
              <h3>Tareas manuales</h3>
              <div formArrayName="manualItems">
                @for (item of manualItems.controls; track item; let i = $index) {
                  <div [formGroupName]="i" class="item-row manual-row">
                    @if (showVisibilityColumn) {
                      <mat-checkbox formControlName="visiblePdf" matTooltip="Visible en el PDF" class="visibility-check"></mat-checkbox>
                    }
                    <mat-form-field appearance="outline" class="desc-wide">
                      <mat-label>Descripción</mat-label>
                      <input matInput formControlName="tareaManual" placeholder="Descripción del trabajo específico">
                    </mat-form-field>
                    <mat-form-field appearance="outline">
                      <mat-label>Cantidad</mat-label>
                      <input matInput type="number" formControlName="cantidad" min="0.001" step="0.01">
                    </mat-form-field>
                    <mat-form-field appearance="outline">
                      <mat-label>Precio unit.</mat-label>
                      <input matInput type="number" formControlName="precioUnitario" min="0" step="0.01">
                    </mat-form-field>
                    <mat-checkbox formControlName="aplicaIva">IVA</mat-checkbox>
                    <button type="button" mat-icon-button color="warn" (click)="removeManualItem(i)" matTooltip="Eliminar">
                      <mat-icon>delete</mat-icon>
                    </button>
                  </div>
                }
              </div>
              <button type="button" mat-stroked-button (click)="addManualTaskItem()">
                <mat-icon>add</mat-icon>
                Añadir tarea manual
              </button>
              <mat-checkbox [(ngModel)]="showVisibilityColumn" [ngModelOptions]="{standalone: true}" class="visibility-toggle">
                Mostrar opciones de visibilidad
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
              <h3>Anticipo / seña (fiscal)</h3>
              <p class="hint-anticipo">
                Registra el cobro del anticipo (seña), emite la factura de anticipo y, al finalizar el trabajo, la factura final con descuento del anticipo ya facturado.
              </p>
              @if (!estadoPermiteRegistrarAnticipoApi()) {
                <p class="hint-estado">
                  Para <strong>registrar por primera vez</strong> un anticipo, el estado del presupuesto debe ser <strong>Aceptado</strong>
                  (puedes cambiarlo arriba). Si ya tenías anticipo registrado, el resumen y las facturas siguen disponibles.
                </p>
              }
              @if (!auth.canMutate()) {
                <p class="hint-estado">Tu cuenta no tiene permiso de edición (solo lectura); no se pueden registrar anticipos ni generar facturas desde aquí.</p>
              }
              @if (resumenAnticipo) {
                <div class="anticipo-resumen-block">
                  @if (!resumenAnticipo.tieneAnticipoRegistrado) {
                    @if (estadoPermiteRegistrarAnticipoApi()) {
                    <div class="discount-fields">
                      <mat-form-field appearance="outline">
                        <mat-label>Importe anticipo (€, IVA incl.)</mat-label>
                        <input matInput type="number" [(ngModel)]="anticipoRegImporte" [ngModelOptions]="{standalone: true}" min="0.01" step="0.01" placeholder="0">
                      </mat-form-field>
                      <mat-form-field appearance="outline">
                        <mat-label>Fecha del anticipo</mat-label>
                        <input matInput type="date" [(ngModel)]="anticipoRegFecha" [ngModelOptions]="{standalone: true}">
                      </mat-form-field>
                      <button type="button" mat-raised-button color="primary" (click)="registrarAnticipoClick()" [disabled]="anticipoCargando || !auth.canMutate()">
                        Registrar anticipo
                      </button>
                    </div>
                    } @else {
                    <p class="text-muted">Cambia el estado a <strong>Aceptado</strong> para indicar importe y fecha del anticipo.</p>
                    }
                  } @else {
                    <div class="resumen-grid">
                      <span>Total presupuesto</span><span>{{ resumenAnticipo.totalPresupuesto | number:'1.2-2' }} €</span>
                      <span>Anticipo (IVA incl.)</span><span>{{ resumenAnticipo.importeAnticipo | number:'1.2-2' }} €</span>
                      <span>Base / IVA anticipo</span><span>{{ resumenAnticipo.baseAnticipo | number:'1.2-2' }} € / {{ resumenAnticipo.ivaAnticipo | number:'1.2-2' }} €</span>
                      <span>Pendiente (neto a facturar en final)</span><span>{{ resumenAnticipo.importePendiente | number:'1.2-2' }} €</span>
                    </div>
                    @if (!resumenAnticipo.anticipoYaFacturado) {
                      <button type="button" mat-raised-button color="primary" (click)="generarFacturaAnticipoClick()" [disabled]="anticipoCargando || !auth.canMutate()" class="anticipo-btn">
                        Generar factura de anticipo
                      </button>
                    } @else if (!facturaPrincipalId) {
                      <button type="button" mat-raised-button color="accent" (click)="confirmarFacturaFinal()" [disabled]="anticipoCargando || !auth.canMutate()" class="anticipo-btn">
                        Generar factura final (restante)
                      </button>
                    } @else {
                      <p class="hint-ok">Ya existe factura de venta principal (n.º enlazado en el listado).</p>
                    }
                  }
                </div>
              } @else if (anticipoResumenLoading) {
                <p class="text-muted">Cargando resumen…</p>
              } @else {
                <p class="text-muted">No se pudo cargar el resumen de anticipo.</p>
              }
            </div>
            } @else {
            <div class="section anticipo-section anticipo-cartel-solo">
              <h3>Anticipo / seña</h3>
              <p class="hint-anticipo">
                Cuando el presupuesto pase a <strong>Aceptado</strong> o <strong>En ejecución</strong>, aquí podrás registrar el anticipo cobrado,
                generar la factura de anticipo y la factura final con el descuento correspondiente.
              </p>
            </div>
            }
            }

            <!-- 4. Descuentos -->
            <div class="section discount-section">
              <h3>Descuentos globales</h3>
              <div class="discount-fields">
                <mat-form-field appearance="outline">
                  <mat-label>Descuento %</mat-label>
                  <input matInput type="number" formControlName="descuentoGlobalPorcentaje" min="0" max="100" step="0.01">
                </mat-form-field>
                <mat-form-field appearance="outline">
                  <mat-label>Descuento (€)</mat-label>
                  <input matInput type="number" formControlName="descuentoGlobalFijo" min="0" step="0.01">
                </mat-form-field>
                <mat-checkbox formControlName="descuentoAntesIva">Aplicar descuento antes del IVA</mat-checkbox>
              </div>
            </div>

            <!-- 5. Resumen de costes -->
            <div class="section cost-summary">
              <h3>Resumen de costes</h3>
              <div class="summary-rows">
                <div class="summary-row">
                  <span>Subtotal ítems:</span>
                  <span>{{ costesResumen.subtotalItems | number:'1.2-2' }} €</span>
                </div>
                @if (costesResumen.descuentoPorcentaje > 0 || costesResumen.descuentoFijo > 0) {
                  <div class="summary-row discount">
                    <span>Descuento aplicado:</span>
                    <span>- {{ costesResumen.descuentoTotal | number:'1.2-2' }} €</span>
                  </div>
                }
                <div class="summary-row">
                  <span>Base IVA:</span>
                  <span>{{ costesResumen.baseIva | number:'1.2-2' }} €</span>
                </div>
                @if (form.get('ivaHabilitado')?.value) {
                  <div class="summary-row">
                    <span>IVA (21%):</span>
                    <span>{{ costesResumen.iva | number:'1.2-2' }} €</span>
                  </div>
                }
                <div class="summary-row total">
                  <span>Total:</span>
                  <span>{{ costesResumen.total | number:'1.2-2' }} €</span>
                </div>
              </div>
            </div>

            <div class="actions">
              <button mat-button type="button" routerLink="/presupuestos">Cancelar</button>
              <button
                mat-raised-button
                color="primary"
                type="submit"
                [disabled]="botonCrearDeshabilitado()"
              >
                {{ isEdit ? 'Guardar' : 'Crear' }}
              </button>
            </div>
          </form>
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
  `]
})
export class PresupuestoFormComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);

  form: FormGroup;
  /** Catálogo de condiciones (solo claves + textos desde API). */
  condicionesCatalogo: PresupuestoCondicionDisponible[] = [];
  clientes: Cliente[] = [];
  materiales: Material[] = [];
  topMateriales: Material[] = [];
  showVisibilityColumn = false;
  isEdit = false;
  id?: number;
  /** existente: desplegable; nuevo: solo nombre y alta rápida. */
  clienteModo: 'existente' | 'nuevo' = 'existente';
  nombreClienteNuevo = '';

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

  get manualItems(): FormArray {
    return this.form.get('manualItems') as FormArray;
  }

  get costesResumen() {
    return this.calcularCostesResumen();
  }

  /** El botón no depende de filas vacías: la validación fuerte está en onSubmit. */
  botonCrearDeshabilitado(): boolean {
    return !this.auth.canMutate() || this.form.pending;
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
    if (id && id !== 'nuevo') {
      this.isEdit = true;
      this.id = +id;
      forkJoin({
        p: this.presupuestoService.getById(this.id),
        disp: this.presupuestoService.getCondicionesDisponibles(),
      }).subscribe({
        next: ({ p, disp }) => {
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
      `Total presupuesto: ${r.totalPresupuesto.toFixed(2)} €`,
      `Importe pendiente (a cobrar en la final): ${r.importePendiente.toFixed(2)} €`,
      '',
      '¿Emitir la factura final con descuento del anticipo ya facturado?',
    ].join('\n');
    this.dialog
      .open(ConfirmDialogComponent, {
        width: '440px',
        data: {
          title: 'Factura final con anticipo',
          message: msg,
          confirmLabel: 'Generar factura final',
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
    materialId: number | null;
    tareaManual: string;
    cantidad: number;
    precioUnitario: number;
    aplicaIva: boolean;
    descuentoPorcentaje: number;
    descuentoFijo: number;
    visiblePdf: boolean;
    isManualTask: boolean;
  }): FormGroup {
    return this.fb.group({
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

  private calcularCostesResumen(): {
    subtotalItems: number;
    descuentoPorcentaje: number;
    descuentoFijo: number;
    descuentoTotal: number;
    baseIva: number;
    iva: number;
    total: number;
  } {
    const allItems = this.getAllItems();
    let subtotalItems = 0;
    let baseIva = 0;
    for (const { ctrl } of allItems) {
      const v = ctrl.value;
      const cantidad = +(v.cantidad ?? 0);
      const precio = +(v.precioUnitario ?? 0);
      const descPct = +(v.descuentoPorcentaje ?? 0);
      const descFijo = +(v.descuentoFijo ?? 0);
      let itemSub = cantidad * precio;
      itemSub = itemSub * (1 - descPct / 100) - descFijo;
      itemSub = Math.max(0, itemSub);
      subtotalItems += itemSub;
      if (v.aplicaIva) baseIva += itemSub;
    }
    const descPct = +(this.form.get('descuentoGlobalPorcentaje')?.value ?? 0);
    const descFijo = +(this.form.get('descuentoGlobalFijo')?.value ?? 0);
    const descuentoAntesIva = this.form.get('descuentoAntesIva')?.value !== false;
    let subtotal = subtotalItems;
    if (descuentoAntesIva) {
      subtotal = subtotal * (1 - descPct / 100) - descFijo;
      baseIva = baseIva * (1 - descPct / 100) - descFijo;
    } else {
      subtotal = subtotal * (1 - descPct / 100) - descFijo;
    }
    subtotal = Math.max(0, subtotal);
    baseIva = Math.max(0, baseIva);
    const descuentoTotal = subtotalItems - subtotal;
    const ivaHabilitado = this.form.get('ivaHabilitado')?.value !== false;
    const iva = ivaHabilitado ? baseIva * IVA_RATE : 0;
    const total = subtotal + iva;
    return {
      subtotalItems,
      descuentoPorcentaje: descPct,
      descuentoFijo: descFijo,
      descuentoTotal,
      baseIva,
      iva,
      total,
    };
  }

  onSubmit(): void {
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
        cantidad: +it.cantidad,
        precioUnitario: +it.precioUnitario,
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
    req.subscribe({
      next: (presupuesto) => {
        this.snackBar.open(
          this.translate.instant(this.isEdit ? 'snack.budgetUpdated' : 'snack.budgetSavedCreated'),
          this.translate.instant('common.close'),
          { duration: 3000 },
        );
        this.presupuestoService.downloadPdf(presupuesto.id).subscribe({
          next: (blob) => {
            const url = URL.createObjectURL(blob);
            window.open(url, '_blank');
            URL.revokeObjectURL(url);
          },
          error: () => {},
        });
        this.router.navigate(['/presupuestos']);
      },
      error: (err) => {
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
