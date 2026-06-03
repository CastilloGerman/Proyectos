import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

export interface HintStep {
  icon: string;
  text: string;
}

@Component({
  selector: 'app-hint-banner',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule],
  template: `
    <div class="hint-banner" *ngIf="visible" role="note" aria-label="Consejo de uso">
      <div class="hint-header">
        <mat-icon class="hint-icon">tips_and_updates</mat-icon>
        <span class="hint-title">{{ title }}</span>
        <button mat-icon-button class="hint-close" (click)="dismiss()" aria-label="Cerrar consejo">
          <mat-icon>close</mat-icon>
        </button>
      </div>
      <ol class="hint-steps">
        <li *ngFor="let step of steps; let i = index" class="hint-step">
          <span class="step-number">{{ i + 1 }}</span>
          <mat-icon class="step-icon">{{ step.icon }}</mat-icon>
          <span class="step-text">{{ step.text }}</span>
        </li>
      </ol>
    </div>
  `,
  styles: [`
    .hint-banner {
      margin-bottom: 20px;
      padding: 16px 20px;
      border-radius: 12px;
      background: rgba(107, 63, 160, 0.07);
      border: 1px solid rgba(107, 63, 160, 0.22);
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
      margin-bottom: 12px;
    }

    .hint-icon {
      color: #6B3FA0;
      font-size: 20px;
      width: 20px;
      height: 20px;
    }

    .hint-title {
      flex: 1;
      font-size: 14px;
      font-weight: 600;
      color: #6B3FA0;
    }

    .hint-close {
      width: 28px;
      height: 28px;
      line-height: 28px;
    }

    .hint-close mat-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
      color: #6B3FA0;
      opacity: 0.7;
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
      background: rgba(107, 63, 160, 0.15);
      color: #6B3FA0;
      font-size: 11px;
      font-weight: 700;
      flex-shrink: 0;
    }

    .step-icon {
      font-size: 17px;
      width: 17px;
      height: 17px;
      color: #6B3FA0;
      opacity: 0.75;
      flex-shrink: 0;
    }

    .step-text {
      line-height: 1.4;
    }

    :host-context(html.app-dark-theme) .hint-banner {
      background: rgba(107, 63, 160, 0.14);
      border-color: rgba(107, 63, 160, 0.38);
    }
    :host-context(html.app-dark-theme) .hint-title,
    :host-context(html.app-dark-theme) .hint-icon,
    :host-context(html.app-dark-theme) .step-number,
    :host-context(html.app-dark-theme) .step-icon {
      color: #c4a0e8;
    }
    :host-context(html.app-dark-theme) .step-number {
      background: rgba(196, 160, 232, 0.18);
    }
    :host-context(html.app-dark-theme) .hint-close mat-icon {
      color: #c4a0e8;
    }
  `]
})
export class HintBannerComponent implements OnInit {
  /** Clave única para localStorage — cada pantalla tiene la suya */
  @Input() storageKey!: string;
  /** Título del banner */
  @Input() title: string = '¿Cómo funciona?';
  /** Máximo 3 pasos */
  @Input() steps: HintStep[] = [];

  visible = false;

  ngOnInit(): void {
    this.visible = localStorage.getItem(this.storageKey) !== 'dismissed';
  }

  dismiss(): void {
    this.visible = false;
    localStorage.setItem(this.storageKey, 'dismissed');
  }
}
