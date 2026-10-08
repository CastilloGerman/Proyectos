import { AfterViewInit, Component, DestroyRef, ElementRef, EventEmitter, Input, Output, ViewChild, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { PresupuestoIaBorradorResponse, PresupuestoIaRequestError } from '../../core/models/presupuesto-ia.model';
import { PresupuestoIaService } from '../../core/services/presupuesto-ia.service';

export interface PresupuestoIaPanelState {
  loading: boolean;
  statusMessage: string;
  errorMessage: string;
  errorKind: string | null;
}

@Component({
  selector: 'app-presupuesto-ia-panel',
  standalone: true,
  imports: [FormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, TranslateModule],
  template: `
    <section class="ia-panel" aria-labelledby="ia-panel-title">
      <h2 id="ia-panel-title">{{ 'budQuick.ai.title' | translate }}</h2>
      @if (warningMessage) {
        <p class="overwrite-warning" role="alert" aria-live="assertive">{{ warningMessage }}</p>
      }
      <mat-form-field appearance="outline" class="full">
        <mat-label>{{ 'budQuick.ai.descriptionLabel' | translate }}</mat-label>
        <textarea #descriptionInput matInput [ngModel]="text" (ngModelChange)="textChange.emit($event)"
          [ngModelOptions]="{standalone: true}" maxlength="8000" rows="4"
          aria-describedby="ia-char-count ia-status"></textarea>
      </mat-form-field>
      <p class="price-tip"><mat-icon aria-hidden="true">tips_and_updates</mat-icon>
        {{ 'budQuick.ai.priceTip' | translate }}
      </p>
      <div class="actions">
        <span id="ia-char-count" class="char-count">{{ 'budQuick.ai.charCount' | translate:{count: text.length} }}</span>
        <button type="button" mat-raised-button color="primary" class="ai-generate" (click)="generarBorrador()"
          [disabled]="loading || unavailable || disabled || !text.trim() || text.length > 8000">
          <mat-icon>{{ loading ? 'sync' : 'auto_awesome' }}</mat-icon>
          {{ (loading ? 'budQuick.ai.generating' : 'budQuick.ai.generate') | translate }}
        </button>
      </div>
      <p #iaStatus id="ia-status" class="status" [class.error]="!!errorMessage" [hidden]="!statusMessage"
        [attr.role]="errorMessage ? 'alert' : 'status'" aria-live="polite" aria-atomic="true" tabindex="-1">
        {{ statusMessage }}
      </p>
    </section>
  `,
  styles: [`
    .ia-panel { margin: 0; }
    .full { display: block; width: 100%; }
    .actions { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 12px; }
    .char-count { color: var(--app-text-secondary, #64748b); font-size: 13px; }
    .price-tip { display: flex; align-items: flex-start; gap: 8px; margin: 0 0 12px; font-size: 13px; }
    .price-tip mat-icon { flex: 0 0 auto; }
    button { min-height: 48px; }
    .status { margin: 8px 0 0; font-weight: 600; }
    .status.error { color: #b91c1c; }
    .overwrite-warning { margin: 0 0 12px; padding: 10px 12px; border: 1px solid #b45309; border-radius: 8px; color: #92400e; background: #fffbeb; }
  `],
})
export class PresupuestoIaPanelComponent implements AfterViewInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly iaService = inject(PresupuestoIaService);
  private readonly translate = inject(TranslateService);
  @ViewChild('iaStatus') private iaStatus?: ElementRef<HTMLElement>;
  @ViewChild('descriptionInput') private descriptionInput?: ElementRef<HTMLTextAreaElement>;

  @Input() text = '';
  @Input() clienteId: number | null = null;
  @Input() hasExistingItems = false;
  @Input() warningMessage = '';
  @Input() focusOnInit = false;
  @Input() disabled = false;
  loading = false;
  unavailable = false;
  statusMessage = '';
  errorMessage = '';
  @Output() readonly textChange = new EventEmitter<string>();
  @Output() readonly draftGenerated = new EventEmitter<PresupuestoIaBorradorResponse>();
  @Output() readonly stateChange = new EventEmitter<PresupuestoIaPanelState>();

  ngAfterViewInit(): void {
    if (this.focusOnInit) setTimeout(() => this.descriptionInput?.nativeElement.focus(), 0);
  }

  generarBorrador(text = this.text, clienteId = this.clienteId, hasExistingItems = this.hasExistingItems): void {
    const cleanText = text.trim();
    if (!cleanText || cleanText.length > 8000 || this.loading || this.unavailable || this.disabled) return;
    if (hasExistingItems && !window.confirm(this.translate.instant('budQuick.ai.confirmOverwrite'))) return;

    this.errorMessage = '';
    this.loading = true;
    this.statusMessage = this.translate.instant('budQuick.ai.progress');
    this.emitState(null);
    this.iaService.generarBorrador({ texto: cleanText, ...(clienteId == null ? {} : { clienteId }) })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (draft) => {
          this.draftGenerated.emit(draft);
          this.loading = false;
          this.statusMessage = this.translate.instant('budQuick.ai.draftReady');
          this.emitState(null);
          this.focusStatus();
        },
        error: (error: unknown) => {
          this.loading = false;
          const kind = error instanceof PresupuestoIaRequestError ? error.kind : 'unknown';
          this.unavailable = kind === 'disabled';
          this.errorMessage = this.translate.instant(this.errorKey(kind));
          this.statusMessage = this.errorMessage;
          this.emitState(kind);
          this.focusStatus();
        },
      });
  }

  focusStatus(): void {
    this.iaStatus?.nativeElement.focus();
  }

  private emitState(errorKind: string | null): void {
    this.stateChange.emit({
      loading: this.loading,
      statusMessage: this.statusMessage,
      errorMessage: this.errorMessage,
      errorKind,
    });
  }

  private errorKey(kind: string): string {
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
      'invalid-response': 'invalidResponse',
      unknown: 'genericError',
    };
    return `budQuick.ai.${keys[kind] ?? 'genericError'}`;
  }
}
