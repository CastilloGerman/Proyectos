import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ConfigService, SeguimientoPresupuestosConfig } from '../../../core/services/config.service';
import { HintBannerComponent } from '../../../shared/hint-banner/hint-banner.component';

@Component({
  selector: 'app-seguimiento-presupuestos',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, TranslateModule, HintBannerComponent],
  template: `
    <main class="tracking-settings">
      <a routerLink="/dashboard" class="back">{{ 'common.back' | translate }}</a>
      <h1>{{ 'budgetFollowSettings.title' | translate }}</h1>
      <p>{{ 'budgetFollowSettings.subtitle' | translate }}</p>
      <app-hint-banner
        storageKey="budget_follow_settings_hint_v1"
        [title]="'budgetFollowSettings.hintTitle' | translate"
        [steps]="[
          { icon: 'schedule', text: ('budgetFollowSettings.hint1' | translate) },
          { icon: 'notifications_active', text: ('budgetFollowSettings.hint2' | translate) },
          { icon: 'mail', text: ('budgetFollowSettings.hint3' | translate) }
        ]"
        [note]="'budgetFollowSettings.hintNote' | translate"
      />

      @if (loading) {
        <p role="status">{{ 'budgetFollowSettings.loading' | translate }}</p>
      } @else if (loadError) {
        <section class="settings-card">
          <p role="alert">{{ 'budgetFollowSettings.loadError' | translate }}</p>
          <button type="button" (click)="load()">{{ 'budgetFollowSettings.retry' | translate }}</button>
        </section>
      } @else {
        <form class="settings-card" [formGroup]="form" (ngSubmit)="save()">
          <label class="toggle">
            <input type="checkbox" formControlName="seguimientoActivo" />
            <span>{{ 'budgetFollowSettings.enabled' | translate }}</span>
          </label>

          <label>
            <span>{{ 'budgetFollowSettings.waitDays' | translate }}</span>
            <input type="number" min="1" max="30" step="1" formControlName="diasEspera" />
            <small>{{ 'budgetFollowSettings.waitHelp' | translate }}</small>
            @if (form.controls.diasEspera.touched && form.controls.diasEspera.invalid) {
              <span class="error">{{ 'budgetFollowSettings.waitError' | translate }}</span>
            }
          </label>

          <label>
            <span>{{ 'budgetFollowSettings.maxAlerts' | translate }}</span>
            <input type="number" min="1" max="5" step="1" formControlName="maxAvisos" />
            <small>{{ 'budgetFollowSettings.maxHelp' | translate }}</small>
            @if (form.controls.maxAvisos.touched && form.controls.maxAvisos.invalid) {
              <span class="error">{{ 'budgetFollowSettings.maxError' | translate }}</span>
            }
          </label>

          <label class="toggle">
            <input type="checkbox" formControlName="emailResumen" />
            <span>{{ 'budgetFollowSettings.emailDigest' | translate }}</span>
          </label>

          <div class="actions">
            <button type="submit" [disabled]="saving || form.invalid || form.pristine">
              {{ saving ? ('budgetFollowSettings.saving' | translate) : ('budgetFollowSettings.save' | translate) }}
            </button>
          </div>
          @if (feedback) {
            <p [class.error]="feedbackType === 'error'" [class.success]="feedbackType === 'success'" role="status" aria-live="polite">
              {{ feedback }}
            </p>
          }
        </form>
      }
    </main>
  `,
  styles: [`
    .tracking-settings { max-width: 760px; margin: 0 auto; padding: 16px; }
    h1 { margin: 16px 0 8px; }
    .back { display: inline-flex; align-items: center; min-height: 44px; }
    .settings-card { display: grid; gap: 20px; padding: 24px; border: 1px solid var(--app-border,#d1d5db); border-radius: 12px; background: var(--app-bg-card,#fff); }
    .settings-card label:not(.toggle) { display: grid; gap: 6px; font-weight: 600; }
    input[type="number"] { box-sizing: border-box; width: 100%; max-width: 180px; min-height: 44px; padding: 8px 10px; border: 1px solid #87909d; border-radius: 6px; font: inherit; }
    .toggle { display: flex; align-items: center; gap: 12px; min-height: 44px; }
    input[type="checkbox"] { width: 22px; height: 22px; }
    small { font-weight: 400; color: var(--app-text-secondary,#64748b); }
    .actions button { min-height: 48px; padding: 10px 18px; border: 0; border-radius: 8px; color: #fff; background: var(--app-primary,#2563eb); font: inherit; font-weight: 600; cursor: pointer; }
    button:disabled { opacity: .55; cursor: not-allowed; }
    .error { color: #b42318; }
    .success { color: #067647; }
  `],
})
export class SeguimientoPresupuestosComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly config = inject(ConfigService);
  private readonly translate = inject(TranslateService);

  readonly form = this.fb.nonNullable.group({
    seguimientoActivo: [true],
    diasEspera: [3, [Validators.required, Validators.min(1), Validators.max(30), Validators.pattern(/^\d+$/)]],
    maxAvisos: [2, [Validators.required, Validators.min(1), Validators.max(5), Validators.pattern(/^\d+$/)]],
    emailResumen: [false],
  });
  loading = true;
  saving = false;
  loadError = false;
  feedback = '';
  feedbackType: 'success' | 'error' | '' = '';

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.loadError = false;
    this.config.getSeguimientoPresupuestos().subscribe({
      next: value => {
        this.form.patchValue(value);
        this.form.markAsPristine();
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.loadError = true;
      },
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.feedback = '';
    this.feedbackType = '';
    this.config.patchSeguimientoPresupuestos(this.form.getRawValue() as SeguimientoPresupuestosConfig).subscribe({
      next: saved => {
        this.form.patchValue(saved);
        this.form.markAsPristine();
        this.saving = false;
        this.feedbackType = 'success';
        this.feedback = this.translate.instant('budgetFollowSettings.saved');
      },
      error: () => {
        this.saving = false;
        this.feedbackType = 'error';
        this.feedback = this.translate.instant('budgetFollowSettings.saveError');
      },
    });
  }
}
