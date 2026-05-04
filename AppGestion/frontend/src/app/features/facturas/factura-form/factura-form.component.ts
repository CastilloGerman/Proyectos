import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { FacturaService } from '../../../core/services/factura.service';
import { ConfigService } from '../../../core/services/config.service';
import { AuthService } from '../../../core/auth/auth.service';
import { ClienteService } from '../../../core/services/cliente.service';
import { PresupuestoService } from '../../../core/services/presupuesto.service';
import { MaterialService } from '../../../core/services/material.service';
import { Cliente } from '../../../core/models/cliente.model';
import { Presupuesto } from '../../../core/models/presupuesto.model';
import { Material } from '../../../core/models/material.model';
import { Factura, FacturaCobro, FacturaItemRequest, FacturaRequest } from '../../../core/models/factura.model';
import { startWith } from 'rxjs';

@Component({
    selector: 'app-factura-form',
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
        MatDividerModule,
    ],
    template: `
    <div class="factura-form">
      <mat-card>
        <mat-card-header>
          <mat-card-title>{{ isEdit ? 'Editar factura' : 'Nueva factura' }}</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Cliente</mat-label>
              <mat-select formControlName="clienteId" required>
                @for (c of clientes; track c.id) {
                  <mat-option [value]="c.id">{{ c.nombre }}</mat-option>
                }
              </mat-select>
              @if (form.get('clienteId')?.hasError('required') && form.get('clienteId')?.touched) {
                <mat-error>Selecciona un cliente</mat-error>
              }
              @if (form.get('clienteId')?.hasError('nifIgual')) {
                <mat-error>
                  El cliente seleccionado tiene el mismo NIF que tu perfil. Por favor selecciona otro cliente.
                </mat-error>
              }
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Presupuesto (opcional)</mat-label>
              <mat-select formControlName="presupuestoId">
                <mat-option [value]="null">Ninguno</mat-option>
                @for (p of presupuestos; track p.id) {
                  <mat-option [value]="p.id">{{ p.clienteNombre }} - {{ p.total | number:'1.2-2' }} €</mat-option>
                }
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Nº Factura</mat-label>
              <input matInput formControlName="numeroFactura" placeholder="Se genera automático si se deja vacío">
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Fecha expedición</mat-label>
              <input matInput formControlName="fechaExpedicion" type="date">
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Fecha operación (opcional)</mat-label>
              <input matInput formControlName="fechaOperacion" type="date">
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Fecha vencimiento</mat-label>
              <input matInput formControlName="fechaVencimiento" type="date">
              <mat-hint>Opciones rápidas:</mat-hint>
            </mat-form-field>
            <div class="vencimiento-buttons">
              <button type="button" mat-stroked-button (click)="setVencimiento(15)">15 días</button>
              <button type="button" mat-stroked-button (click)="setVencimiento(30)">30 días</button>
              <button type="button" mat-stroked-button (click)="setVencimiento(60)">60 días</button>
              <button type="button" mat-stroked-button (click)="setVencimiento(90)">90 días</button>
            </div>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Régimen fiscal</mat-label>
              <input matInput formControlName="regimenFiscal" placeholder="Régimen general del IVA">
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Condiciones de pago</mat-label>
              <input matInput formControlName="condicionesPago" placeholder="30 días">
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Método de pago</mat-label>
              <mat-select formControlName="metodoPago">
                <mat-option value="Transferencia">Transferencia</mat-option>
                <mat-option value="Bizum">Bizum</mat-option>
                <mat-option value="Efectivo">Efectivo</mat-option>
                <mat-option value="Tarjeta">Tarjeta</mat-option>
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Estado de pago</mat-label>
              <mat-select formControlName="estadoPago">
                <mat-option value="No Pagada">No Pagada</mat-option>
                <mat-option value="Pagada">Pagada</mat-option>
                <mat-option value="Parcial">Parcial</mat-option>
              </mat-select>
            </mat-form-field>
            @if (form.get('estadoPago')?.value === 'Parcial') {
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Importe cobrado (€)</mat-label>
                <input matInput type="number" formControlName="montoCobrado" min="0" step="0.01" placeholder="Importe real cobrado hasta ahora">
                <mat-hint>Introduce el importe ya recibido para un cálculo preciso</mat-hint>
              </mat-form-field>
            }
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Notas</mat-label>
              <textarea matInput formControlName="notas" rows="2"></textarea>
            </mat-form-field>
            <mat-checkbox formControlName="ivaHabilitado">IVA incluido</mat-checkbox>
            <div formArrayName="items" class="items-section">
              <div class="items-header">
                <h3>Líneas</h3>
                <button type="button" mat-stroked-button (click)="addItem()">
                  <mat-icon>add</mat-icon>
                  Añadir línea
                </button>
              </div>
              @for (item of items.controls; track item; let i = $index) {
                <div [formGroupName]="i" class="item-row">
                  <mat-form-field appearance="outline" class="material-select">
                    <mat-label>Material</mat-label>
                    <mat-select formControlName="materialId" (selectionChange)="onMaterialSelect(i, $event.value)">
                      <mat-option [value]="null">Ninguno</mat-option>
                      @for (m of materiales; track m.id) {
                        <mat-option [value]="m.id">{{ m.nombre }} ({{ m.precioUnitario | number:'1.2-2' }} €)</mat-option>
                      }
                    </mat-select>
                  </mat-form-field>
                  <mat-form-field appearance="outline">
                    <mat-label>Descripción</mat-label>
                    <input matInput formControlName="tareaManual" placeholder="Descripción">
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
                  <button type="button" mat-icon-button color="warn" (click)="removeItem(i)">
                    <mat-icon>delete</mat-icon>
                  </button>
                </div>
              }
            </div>
            @if (isEdit && id && auth.canMutate()) {
              <mat-divider class="section-divider"></mat-divider>
              <div class="cobros-section">
                <h3>Cobros parciales</h3>
                <p class="hint-cobros">
                  Registra cada abono; el estado de pago y el importe cobrado se actualizan en el servidor.
                  Total factura: <strong>{{ facturaTotal | number:'1.2-2' }} €</strong> · Cobrado:
                  <strong>{{ montoCobradoServidor | number:'1.2-2' }} €</strong>
                </p>
                @if (cobros.length > 0) {
                  <table class="cobros-table">
                    <thead>
                      <tr>
                        <th>Fecha</th>
                        <th>Importe</th>
                        <th>Método</th>
                        <th>Notas</th>
                      </tr>
                    </thead>
                    <tbody>
                      @for (c of cobros; track c.id) {
                        <tr>
                          <td>{{ c.fecha | date:'dd/MM/yyyy' }}</td>
                          <td>{{ c.importe | number:'1.2-2' }} €</td>
                          <td>{{ c.metodo || '—' }}</td>
                          <td>{{ c.notas || '—' }}</td>
                        </tr>
                      }
                    </tbody>
                  </table>
                }
                <div class="nuevo-cobro-row" [formGroup]="cobroForm">
                  <mat-form-field appearance="outline">
                    <mat-label>Importe (€)</mat-label>
                    <input matInput type="number" formControlName="importe" min="0.01" step="0.01">
                  </mat-form-field>
                  <mat-form-field appearance="outline">
                    <mat-label>Fecha</mat-label>
                    <input matInput type="date" formControlName="fecha">
                  </mat-form-field>
                  <mat-form-field appearance="outline">
                    <mat-label>Método</mat-label>
                    <mat-select formControlName="metodo">
                      <mat-option value="Transferencia">Transferencia</mat-option>
                      <mat-option value="Efectivo">Efectivo</mat-option>
                      <mat-option value="Tarjeta">Tarjeta</mat-option>
                      <mat-option value="Bizum">Bizum</mat-option>
                    </mat-select>
                  </mat-form-field>
                  <mat-form-field appearance="outline" class="cobro-notas">
                    <mat-label>Notas</mat-label>
                    <input matInput formControlName="notas">
                  </mat-form-field>
                  <button type="button" mat-stroked-button color="primary" (click)="addCobro()" [disabled]="cobroForm.invalid || cobroSaving">
                    {{ cobroSaving ? 'Registrando…' : 'Registrar cobro' }}
                  </button>
                </div>
              </div>
              <div class="payment-link-section">
                <h3>Enlace de pago (Stripe)</h3>
                <p class="hint-cobros">Genera una URL para que el cliente pague el importe pendiente con tarjeta.</p>
                @if (paymentLinkUrl) {
                  <div class="link-row">
                    <code class="payment-url">{{ paymentLinkUrl }}</code>
                    <button type="button" mat-stroked-button (click)="copyPaymentLink()">
                      <mat-icon>content_copy</mat-icon>
                      Copiar
                    </button>
                  </div>
                }
                <button type="button" mat-raised-button color="accent" (click)="createPaymentLink()" [disabled]="paymentLinkLoading">
                  {{ paymentLinkLoading ? 'Generando…' : (paymentLinkUrl ? 'Renovar enlace de pago' : 'Generar enlace de pago') }}
                </button>
              </div>
            }

            <div class="actions">
              <button mat-button type="button" routerLink="/facturas">Cancelar</button>
              <button mat-raised-button color="primary" type="submit" [disabled]="form.invalid || form.pending || !auth.canMutate()">
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

    .items-section {
      margin: 24px 0;
      padding: 20px;
      border-radius: 8px;
      border: 1px solid var(--app-border, rgba(15, 23, 42, 0.08));
      background: rgba(30, 58, 138, 0.05);
    }

    :host-context(html.app-dark-theme) .items-section {
      background: rgba(96, 165, 250, 0.1);
      border-color: rgba(96, 165, 250, 0.28);
    }

    .items-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }

    .items-header h3 {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
      color: var(--app-text-primary, #0f172a);
    }

    .item-row {
      display: flex;
      flex-wrap: wrap;
      gap: 16px;
      align-items: center;
      margin-bottom: 16px;
      padding: 16px;
      background: var(--app-bg-card, #fff);
      border-radius: 8px;
      border: 1px solid var(--app-border, rgba(15, 23, 42, 0.12));
    }

    :host-context(html.app-dark-theme) .item-row {
      background: rgba(15, 18, 24, 0.92);
      border-color: rgba(255, 255, 255, 0.1);
    }

    .item-row mat-form-field {
      flex: 1;
      min-width: 150px;
    }

    .item-row .material-select {
      min-width: 200px;
    }

    .item-row mat-checkbox {
      margin: 0 8px;
    }

    .actions {
      display: flex;
      gap: 16px;
      margin-top: 24px;
    }

    .vencimiento-buttons {
      display: flex;
      gap: 8px;
      margin: -8px 0 16px 0;
      flex-wrap: wrap;
    }

    .vencimiento-buttons button {
      font-size: 12px;
    }

    .section-divider {
      margin: 28px 0 20px;
    }

    .cobros-section h3,
    .payment-link-section h3 {
      color: var(--app-text-primary, #0f172a);
    }

    .cobros-section, .payment-link-section {
      margin-bottom: 24px;
    }

    .hint-cobros {
      font-size: 13px;
      color: var(--app-text-secondary, rgba(0, 0, 0, 0.6));
      margin: 0 0 12px;
    }

    .cobros-table {
      width: 100%;
      border-collapse: collapse;
      margin-bottom: 16px;
      font-size: 13px;
    }

    .cobros-table th, .cobros-table td {
      border-bottom: 1px solid var(--app-border, #e0e0e0);
      padding: 8px 10px;
      text-align: left;
      color: var(--app-text-primary, inherit);
    }

    .cobros-table th {
      font-weight: 600;
      color: var(--app-text-secondary, #555);
    }

    .nuevo-cobro-row {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      align-items: flex-end;
    }

    .nuevo-cobro-row mat-form-field {
      min-width: 120px;
    }

    .cobro-notas {
      flex: 1;
      min-width: 180px;
    }

    .link-row {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      align-items: center;
      margin-bottom: 12px;
    }

    .payment-url {
      flex: 1;
      min-width: 200px;
      font-size: 11px;
      word-break: break-all;
      background: rgba(15, 23, 42, 0.06);
      color: var(--app-text-primary, #0f172a);
      padding: 8px 10px;
      border-radius: 4px;
      border: 1px solid var(--app-border, rgba(15, 23, 42, 0.08));
    }

    :host-context(html.app-dark-theme) .payment-url {
      background: rgba(255, 255, 255, 0.06);
    }
  `]
})
export class FacturaFormComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  form: FormGroup;
  cobroForm: FormGroup;
  clientes: Cliente[] = [];
  presupuestos: Presupuesto[] = [];
  materiales: Material[] = [];
  isEdit = false;
  id?: number;
  cobros: FacturaCobro[] = [];
  paymentLinkUrl: string | null = null;
  private facturaTotalSnapshot = 0;
  private montoCobradoSnapshot = 0;
  cobroSaving = false;
  paymentLinkLoading = false;

  /** NIF de la empresa (emisor), para no facturar a uno mismo como cliente. */
  empresaNif: string | null = null;

  get facturaTotal(): number {
    return this.facturaTotalSnapshot;
  }

  get montoCobradoServidor(): number {
    return this.montoCobradoSnapshot;
  }

  get items(): FormArray {
    return this.form.get('items') as FormArray;
  }

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    public auth: AuthService,
    private facturaService: FacturaService,
    private configService: ConfigService,
    private clienteService: ClienteService,
    private presupuestoService: PresupuestoService,
    private materialService: MaterialService,
    private snackBar: MatSnackBar
  ) {
    this.form = this.fb.group({
      clienteId: [null, Validators.required],
      presupuestoId: [null],
      numeroFactura: [''],
      fechaExpedicion: [new Date().toISOString().split('T')[0], Validators.required],
      fechaOperacion: [''],
      fechaVencimiento: [''],
      regimenFiscal: ['Régimen general del IVA'],
      condicionesPago: [''],
      metodoPago: ['Transferencia'],
      estadoPago: ['No Pagada'],
      montoCobrado: [null],
      notas: [''],
      ivaHabilitado: [true],
      items: this.fb.array([], Validators.required),
    });
    const hoy = new Date().toISOString().split('T')[0];
    this.cobroForm = this.fb.group({
      importe: [null, [Validators.required, Validators.min(0.01)]],
      fecha: [hoy, Validators.required],
      metodo: ['Transferencia'],
      notas: [''],
    });
  }

  ngOnInit(): void {
    this.configService.getEmpresa().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((e) => {
      this.empresaNif = e.nif?.trim() || null;
      this.validateClienteNifVsEmpresa();
    });
    this.clienteService.getAll().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((data) => {
      this.clientes = data;
      this.validateClienteNifVsEmpresa();
    });
    this.presupuestoService.getAll().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((data) => (this.presupuestos = data));
    this.materialService.getAll().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((data) => (this.materiales = data));
    this.form
      .get('clienteId')!
      .valueChanges.pipe(startWith(this.form.get('clienteId')!.value), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.validateClienteNifVsEmpresa());

    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const id = params.get('id');
      if (id && id !== 'nuevo') {
        const numId = +id;
        if (isNaN(numId)) {
          this.router.navigate(['/facturas']);
          return;
        }
        this.loadFactura(numId);
      } else {
        this.isEdit = false;
        this.id = undefined;
        this.items.clear();
        this.addItem();
        this.applyPaymentDefaultsFromEmpresa();
        const preCliente = this.route.snapshot.queryParamMap.get('clienteId');
        if (preCliente) {
          const n = +preCliente;
          if (!isNaN(n)) {
            this.form.patchValue({ clienteId: n });
          }
        }
      }
    });
  }

  /** Rellena método, condiciones y notas sugeridas desde /config/empresa (pantalla Métodos de cobro). */
  /** Marca error `nifIgual` en cliente si el DNI coincide con el NIF de la empresa. */
  private validateClienteNifVsEmpresa(): void {
    const ctrl = this.form.get('clienteId');
    if (!ctrl) return;
    const prev = ctrl.errors;
    const rest: Record<string, unknown> = prev ? { ...prev } : {};
    delete rest['nifIgual'];
    const hasRest = Object.keys(rest).length > 0;
    const emp = this.empresaNif?.trim();
    const id = ctrl.value as number | null;
    const c = id != null ? this.clientes.find((x) => x.id === id) : undefined;
    const cliDni = c?.dni?.trim();
    const mismo = !!(emp && cliDni && emp.toUpperCase() === cliDni.toUpperCase());
    if (mismo) {
      ctrl.setErrors({ ...rest, nifIgual: true });
    } else {
      ctrl.setErrors(hasRest ? rest : null);
    }
  }

  private applyPaymentDefaultsFromEmpresa(): void {
    this.configService.getEmpresa().subscribe({
      next: (e) => {
        const patch: Record<string, string> = {};
        if (e.defaultMetodoPago) patch['metodoPago'] = e.defaultMetodoPago;
        if (e.defaultCondicionesPago) patch['condicionesPago'] = e.defaultCondicionesPago;
        if (e.regimenIvaPrincipal?.trim()) patch['regimenFiscal'] = e.regimenIvaPrincipal.trim();
        const notasBits: string[] = [];
        const metodo = e.defaultMetodoPago || '';
        if (e.bizumTelefono?.trim() && metodo === 'Bizum') {
          notasBits.push(`Pago con Bizum al ${e.bizumTelefono}`);
        }
        if (notasBits.length) patch['notas'] = notasBits.join('\n');
        if (Object.keys(patch).length) this.form.patchValue(patch);
      },
      error: () => {},
    });
  }

  private loadFactura(id: number): void {
    this.isEdit = true;
    this.id = id;
    this.facturaService.getById(id).subscribe({
      next: (f) => {
        if (f.anulada) {
          this.snackBar.open('Esta factura está anulada y no se puede editar.', 'Cerrar', { duration: 5000 });
          void this.router.navigate(['/facturas']);
          return;
        }
        this.form.patchValue({
          clienteId: f.clienteId,
          presupuestoId: f.presupuestoId ?? null,
          numeroFactura: f.numeroFactura,
          fechaExpedicion: f.fechaExpedicion || new Date().toISOString().split('T')[0],
          fechaOperacion: f.fechaOperacion || '',
          fechaVencimiento: f.fechaVencimiento || '',
          regimenFiscal: f.regimenFiscal || 'Régimen general del IVA',
          condicionesPago: f.condicionesPago || '',
          metodoPago: f.metodoPago,
          estadoPago: f.estadoPago,
          montoCobrado: f.montoCobrado ?? null,
          notas: f.notas || '',
          ivaHabilitado: f.ivaHabilitado,
        });
        this.items.clear();
        const itemList = f.items ?? [];
        itemList.forEach((it) =>
          this.items.push(
            this.fb.group({
              materialId: [it.materialId ?? null],
              tareaManual: [it.descripcion || ''],
              cantidad: [it.cantidad, [Validators.required, Validators.min(0.001)]],
              precioUnitario: [it.precioUnitario, [Validators.required, Validators.min(0)]],
              aplicaIva: [it.aplicaIva ?? true],
            })
          )
        );
        if (itemList.length === 0) {
          this.addItem();
        }
        this.applyFacturaCobrosSnapshot(f);
        this.validateClienteNifVsEmpresa();
      },
      error: (err) => {
        const msg = err.error?.detail ?? err.error?.message ?? 'No se pudo cargar la factura';
        this.snackBar.open(msg, 'Cerrar', { duration: 4000 });
        this.router.navigate(['/facturas']);
      },
    });
  }

  addItem(): void {
    this.items.push(
      this.fb.group({
        materialId: [null],
        tareaManual: ['', Validators.required],
        cantidad: [1, [Validators.required, Validators.min(0.001)]],
        precioUnitario: [0, [Validators.required, Validators.min(0)]],
        aplicaIva: [true],
      })
    );
  }

  removeItem(index: number): void {
    this.items.removeAt(index);
  }

  private applyFacturaCobrosSnapshot(f: Factura): void {
    this.cobros = f.cobros ?? [];
    this.paymentLinkUrl = f.paymentLinkUrl ?? null;
    this.facturaTotalSnapshot = f.total ?? 0;
    this.montoCobradoSnapshot = f.montoCobrado ?? 0;
  }

  addCobro(): void {
    if (!this.id || this.cobroForm.invalid) {
      this.cobroForm.markAllAsTouched();
      return;
    }
    const v = this.cobroForm.value;
    this.cobroSaving = true;
    this.facturaService
      .registrarCobro(this.id, {
        importe: +v.importe,
        fecha: v.fecha || undefined,
        metodo: v.metodo || undefined,
        notas: v.notas?.trim() || undefined,
      })
      .subscribe({
        next: (f) => {
          this.applyFacturaCobrosSnapshot(f);
          this.form.patchValue({
            estadoPago: f.estadoPago,
            montoCobrado: f.montoCobrado ?? null,
          });
          const hoy = new Date().toISOString().split('T')[0];
          this.cobroForm.reset({ importe: null, fecha: hoy, metodo: 'Transferencia', notas: '' });
          this.snackBar.open('Cobro registrado', 'Cerrar', { duration: 3000 });
          this.cobroSaving = false;
        },
        error: (err) => {
          this.snackBar.open(err.error?.message || 'Error al registrar cobro', 'Cerrar', { duration: 4000 });
          this.cobroSaving = false;
        },
      });
  }

  createPaymentLink(): void {
    if (!this.id) return;
    this.paymentLinkLoading = true;
    this.facturaService.generarEnlacePago(this.id).subscribe({
      next: (f) => {
        this.applyFacturaCobrosSnapshot(f);
        this.snackBar.open('Enlace generado. Compártelo con el cliente.', 'Cerrar', { duration: 4000 });
        this.paymentLinkLoading = false;
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'No se pudo generar el enlace', 'Cerrar', { duration: 5000 });
        this.paymentLinkLoading = false;
      },
    });
  }

  copyPaymentLink(): void {
    if (!this.paymentLinkUrl) return;
    navigator.clipboard.writeText(this.paymentLinkUrl).then(
      () => this.snackBar.open('Enlace copiado', 'Cerrar', { duration: 2000 }),
      () => this.snackBar.open('No se pudo copiar', 'Cerrar', { duration: 2000 })
    );
  }

  setVencimiento(dias: number): void {
    const hoy = new Date();
    hoy.setDate(hoy.getDate() + dias);
    const fecha = hoy.toISOString().split('T')[0];
    this.form.patchValue({ fechaVencimiento: fecha });
  }

  onMaterialSelect(index: number, materialId: number | null): void {
    if (materialId == null) return;
    const material = this.materiales.find((m) => m.id === materialId);
    if (material) {
      const item = this.items.at(index);
      item.patchValue({
        tareaManual: material.nombre,
        precioUnitario: material.precioUnitario,
      });
    }
  }

  onSubmit(): void {
    if (this.form.invalid || this.items.length === 0) {
      this.form.markAllAsTouched();
      this.snackBar.open('Completa todos los campos y añade al menos una línea', 'Cerrar', { duration: 4000 });
      return;
    }
    const value = this.form.value;
    const rowItems = (value.items ?? []) as Array<{
      materialId?: number | null;
      tareaManual?: string;
      cantidad: number | string;
      precioUnitario: number | string;
      aplicaIva?: boolean;
    }>;
    const items: FacturaItemRequest[] = rowItems.map((it) => ({
      materialId: it.materialId || undefined,
      tareaManual: it.tareaManual || undefined,
      cantidad: +it.cantidad,
      precioUnitario: +it.precioUnitario,
      aplicaIva: it.aplicaIva,
    }));
    const payload: FacturaRequest = {
      clienteId: value.clienteId as number,
      items,
      metodoPago: value.metodoPago,
      estadoPago: value.estadoPago,
      montoCobrado: value.estadoPago === 'Parcial' && value.montoCobrado != null ? +value.montoCobrado : undefined,
      notas: value.notas || undefined,
      ivaHabilitado: value.ivaHabilitado,
      fechaExpedicion: value.fechaExpedicion || new Date().toISOString().split('T')[0],
    };
    if (value.presupuestoId) payload.presupuestoId = value.presupuestoId;
    if (value.numeroFactura?.trim()) payload.numeroFactura = value.numeroFactura.trim();
    if (value.fechaOperacion) payload.fechaOperacion = value.fechaOperacion || undefined;
    if (value.fechaVencimiento) payload.fechaVencimiento = value.fechaVencimiento;
    if (value.regimenFiscal) payload.regimenFiscal = value.regimenFiscal;
    if (value.condicionesPago) payload.condicionesPago = value.condicionesPago || undefined;
    const req = this.isEdit && this.id
      ? this.facturaService.update(this.id, payload)
      : this.facturaService.create(payload);
    req.subscribe({
      next: () => {
        this.snackBar.open(this.isEdit ? 'Factura actualizada' : 'Factura creada', 'Cerrar', { duration: 3000 });
        this.router.navigate(['/facturas']);
      },
      error: (err) => {
        const msg =
          err.status === 400
            ? err.error?.message || err.error?.detail || 'Error al guardar'
            : err.error?.message || 'Error al guardar';
        this.snackBar.open(msg, 'Cerrar', { duration: 5000 });
      },
    });
  }
}
