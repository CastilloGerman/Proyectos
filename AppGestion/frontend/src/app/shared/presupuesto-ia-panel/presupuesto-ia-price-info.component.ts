import { Component, Input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-presupuesto-ia-price-info',
  standalone: true,
  imports: [DecimalPipe, MatIconModule, TranslateModule],
  template: `
    @if (priceOrigin === 'dictado') {
      <p class="price-badge">
        <mat-icon aria-hidden="true">{{ approximate ? 'functions' : 'record_voice_over' }}</mat-icon>
        {{ (approximate ? 'budQuick.ai.approximatePrice' : 'budQuick.ai.dictatedPrice') | translate }}
      </p>
    }
    @if (hasDifferentCatalogPrice()) {
      <p class="catalog-price">
        <mat-icon aria-hidden="true">inventory_2</mat-icon>
        {{ 'budQuick.ai.catalogPrice' | translate }}: {{ catalogPrice | number:'1.2-2' }} €
      </p>
    }
    @if (includedInPreviousLine) {
      <p class="previous-line-note">
        <mat-icon aria-hidden="true">info</mat-icon>
        {{ 'budQuick.ai.priceIncludedInPreviousLine' | translate }}
      </p>
    }
  `,
  styles: [`
    :host { display: block; }
    .price-badge, .catalog-price, .previous-line-note { display: flex; align-items: center; gap: 6px; margin: 4px 0; font-weight: 600; }
    .price-badge { color: #14532d; }
    .catalog-price { color: var(--app-text-secondary, #64748b); font-size: 13px; }
    .previous-line-note { color: #92400e; font-size: 13px; }
    mat-icon { width: 18px; height: 18px; font-size: 18px; }
  `],
})
export class PresupuestoIaPriceInfoComponent {
  @Input() priceOrigin: 'dictado' | 'catalogo' | 'ninguno' | undefined;
  @Input() approximate = false;
  @Input() catalogPrice: number | null | undefined;
  @Input() currentPrice: number | null | undefined;
  @Input() includedInPreviousLine = false;

  hasDifferentCatalogPrice(): boolean {
    return typeof this.catalogPrice === 'number' && Number.isFinite(this.catalogPrice) && this.catalogPrice > 0
      && typeof this.currentPrice === 'number' && Number.isFinite(this.currentPrice)
      && Math.round(this.catalogPrice * 100) !== Math.round(this.currentPrice * 100);
  }
}
