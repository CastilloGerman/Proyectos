import { Component, OnInit, inject, signal } from '@angular/core';
import { ThemeService } from '../../../core/theme/theme.service';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService, UsuarioResponse } from '../../../core/auth/auth.service';
import { CURRENCY_OPTIONS, LOCALE_OPTIONS, TIMEZONE_OPTIONS } from './preferencias-options';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { LanguageSwitcherComponent } from '../../../shared/language-switcher/language-switcher.component';

@Component({
    selector: 'app-preferencias',
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        RouterLink,
        MatCardModule,
        MatFormFieldModule,
        MatSelectModule,
        MatButtonModule,
        MatIconModule,
        MatProgressSpinnerModule,
        MatSnackBarModule,
        MatSlideToggleModule,
        MatDividerModule,
        TranslateModule,
        LanguageSwitcherComponent,
    ],
    templateUrl: './preferencias.component.html',
    styleUrl: './preferencias.component.scss'
})
export class PreferenciasComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly translate = inject(TranslateService);
  readonly theme = inject(ThemeService);

  readonly locales = LOCALE_OPTIONS;
  /** Incluye moneda guardada aunque no esté en la lista corta. */
  readonly currencyOptions = signal<ReadonlyArray<{ value: string; label: string }>>(CURRENCY_OPTIONS);
  /** Incluye la zona guardada aunque no esté en la lista corta. */
  readonly timezoneOptions = signal<ReadonlyArray<{ value: string; label: string }>>(TIMEZONE_OPTIONS);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly loadError = signal(false);
  readonly me = signal<UsuarioResponse | null>(null);

  readonly form = this.fb.nonNullable.group({
    locale: ['es', Validators.required],
    timeZone: ['Europe/Madrid', Validators.required],
    currencyCode: ['EUR', [Validators.required, Validators.pattern(/^[A-Z]{3}$/i)]],
  });

  ngOnInit(): void {
    this.loadError.set(false);
    this.loading.set(true);
    this.auth.refreshUser().subscribe({
      next: (data) => {
        if (data) {
          this.me.set(data);
          const tz = data.timeZone ?? 'Europe/Madrid';
          let tzOpts: ReadonlyArray<{ value: string; label: string }> = TIMEZONE_OPTIONS;
          if (!TIMEZONE_OPTIONS.some((o) => o.value === tz)) {
            tzOpts = [{ value: tz, label: `${tz} (guardada)` }, ...TIMEZONE_OPTIONS];
          }
          this.timezoneOptions.set(tzOpts);
          const cur = (data.currencyCode ?? 'EUR').toUpperCase();
          let curOpts: ReadonlyArray<{ value: string; label: string }> = CURRENCY_OPTIONS;
          if (!CURRENCY_OPTIONS.some((o) => o.value === cur)) {
            curOpts = [{ value: cur, label: `${cur} (guardada)` }, ...CURRENCY_OPTIONS];
          }
          this.currencyOptions.set(curOpts);
          this.form.patchValue({
            locale: data.locale ?? 'es',
            timeZone: tz,
            currencyCode: cur,
          });
          this.form.markAsPristine();
        } else {
          this.loadError.set(true);
        }
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set(true);
        this.loading.set(false);
      },
    });
  }

  guardar(): void {
    if (this.form.invalid || this.form.pristine) return;
    const v = this.form.getRawValue();
    this.saving.set(true);
    this.auth
      .updatePreferences({
        locale: v.locale,
        timeZone: v.timeZone,
        currencyCode: v.currencyCode.toUpperCase(),
      })
      .subscribe({
        next: (updated) => {
          this.me.set(updated);
          this.form.patchValue(
            {
              locale: updated.locale ?? 'es',
              timeZone: updated.timeZone ?? 'Europe/Madrid',
              currencyCode: (updated.currencyCode ?? 'EUR').toUpperCase(),
            },
            { emitEvent: false }
          );
          this.form.markAsPristine();
          this.snackBar.open(this.translate.instant('snack.prefsSaved'), this.translate.instant('common.close'), {
            duration: 3000,
          });
          this.saving.set(false);
        },
        error: (err) => {
          this.saving.set(false);
          const raw = err.error?.message || err.error?.detail;
          const msg =
            typeof raw === 'string' && raw.trim() !== ''
              ? raw.trim()
              : this.translate.instant('snack.prefsSaveFail');
          this.snackBar.open(msg, this.translate.instant('common.close'), { duration: 5000 });
        },
      });
  }

  reintentar(): void {
    this.ngOnInit();
  }
}
