import { Component, Input, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

export interface HintStep {
  icon: string;
  text: string;
  textKey?: string;
}

@Component({
  selector: 'app-hint-banner',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule, TranslateModule],
  template: `
    <div class="hint-banner" role="note" [attr.aria-label]="ariaLabel">
      <button type="button" class="hint-header" [attr.aria-expanded]="!collapsed" [attr.aria-controls]="'hintBody_' + storageKey" (click)="toggle()">
        <mat-icon class="hint-icon">tips_and_updates</mat-icon>
        <span class="hint-title">{{ title }}</span>
        <mat-icon class="hint-chevron">{{ collapsed ? 'expand_less' : 'expand_more' }}</mat-icon>
      </button>
      <div #hintBody class="hint-body" [id]="'hintBody_' + storageKey" *ngIf="!collapsed">
        <ol class="hint-steps">
          <li *ngFor="let step of steps; let i = index" class="hint-step">
            <span class="step-number">{{ i + 1 }}</span>
            <mat-icon class="step-icon">{{ step.icon }}</mat-icon>
            <span class="step-text">{{ step.textKey ? (step.textKey | translate) : step.text }}</span>
          </li>
        </ol>
        <p class="hint-note" *ngIf="note">{{ note }}</p>
      </div>
    </div>
  `,
  styles: [`
    .hint-banner {
      margin-bottom: 20px;
      padding: 16px 20px;
      border-radius: 12px;
      background: color-mix(in srgb, var(--app-primary) 6%, var(--app-bg-card));
      border: 1px solid color-mix(in srgb, var(--app-primary) 20%, transparent);
      animation: hint-in 0.3s ease;
    }

    @keyframes hint-in {
      from { opacity: 0; transform: translateY(-6px); }
      to   { opacity: 1; transform: translateY(0); }
    }

    .hint-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin: 0;
      padding: 0;
      border: none;
      background: transparent;
      cursor: pointer;
      width: 100%;
      text-align: left;
      font: inherit;
      color: inherit;
    }

    .hint-icon {
      color: var(--app-primary);
      font-size: 20px;
      width: 20px;
      height: 20px;
    }

    .hint-title {
      flex: 1;
      font-size: 14px;
      font-weight: 600;
      color: var(--app-primary);
    }

    .hint-chevron {
      color: var(--app-primary);
      font-size: 20px;
      width: 20px;
      height: 20px;
      opacity: 0.7;
      transition: transform 0.2s ease;
    }

    .hint-body {
      padding-top: 12px;
    }

    .hint-steps {
      list-style: none;
      margin: 0;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .hint-step {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 13.5px;
      color: var(--app-text-primary, #0f172a);
    }

    .step-number {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 22px;
      height: 22px;
      border-radius: 50%;
      background: color-mix(in srgb, var(--app-primary) 15%, transparent);
      color: var(--app-primary);
      font-size: 11px;
      font-weight: 700;
      flex-shrink: 0;
    }

    .step-icon {
      font-size: 17px;
      width: 17px;
      height: 17px;
      color: var(--app-primary);
      opacity: 0.75;
      flex-shrink: 0;
    }

    .step-text {
      line-height: 1.4;
    }

    .hint-note {
      margin: 10px 0 0 54px;
      font-size: 12px;
      line-height: 1.4;
      color: var(--app-text-secondary, #64748b);
    }
  `]
})
export class HintBannerComponent implements OnInit {
  private readonly translate = inject(TranslateService);

  /** Clave única para localStorage — cada pantalla tiene la suya */
  @Input() storageKey!: string;
  /** Título del banner */
  @Input() title: string = '';
  /** Máximo 3 pasos */
  @Input() steps: HintStep[] = [];
  @Input() note = '';
  /** Aria-label traducido */
  @Input() ariaLabel: string = '';

  collapsed = false;

  ngOnInit(): void {
    const stored = localStorage.getItem(this.storageKey);
    this.collapsed = stored === 'dismissed' || stored === 'collapsed';
    if (!this.title) {
      this.title = this.translate.instant('hintBanner.titleDefault');
    }
    if (!this.ariaLabel) {
      this.ariaLabel = this.translate.instant('hintBanner.ariaLabel');
    }
  }

  toggle(): void {
    this.collapsed = !this.collapsed;
    localStorage.setItem(this.storageKey, this.collapsed ? 'collapsed' : 'expanded');
  }
}
