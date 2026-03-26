import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
    selector: 'app-skeleton',
    imports: [CommonModule],
    template: `
    <div class="skeleton-wrapper">
      @for (i of rowsArray; track i) {
        <div class="skeleton-line"></div>
      }
    </div>
  `,
    styles: [`
    .skeleton-wrapper {
      padding: 16px;
      background: #fff;
      border-radius: 1rem;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
    }

    .skeleton-line {
      height: 48px;
      margin-bottom: 8px;
      border-radius: 8px;
      background: linear-gradient(
        90deg,
        #e5e7eb 0%,
        #f3f4f6 50%,
        #e5e7eb 100%
      );
      background-size: 200% 100%;
      animation: skeleton-shimmer 1.2s ease-in-out infinite;
    }

    .skeleton-line:last-child {
      margin-bottom: 0;
    }

    @keyframes skeleton-shimmer {
      0% { background-position: 200% 0; }
      100% { background-position: -200% 0; }
    }
  `]
})
export class SkeletonComponent {
  @Input() rows = 10;

  get rowsArray(): number[] {
    return Array.from({ length: this.rows }, (_, i) => i);
  }
}
