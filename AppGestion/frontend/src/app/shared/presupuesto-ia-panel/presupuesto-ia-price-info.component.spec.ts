import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it } from 'vitest';
import { PresupuestoIaPriceInfoComponent } from './presupuesto-ia-price-info.component';

describe('PresupuestoIaPriceInfoComponent', () => {
  let fixture: ComponentFixture<PresupuestoIaPriceInfoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PresupuestoIaPriceInfoComponent, TranslateModule.forRoot()],
    }).compileComponents();
    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('es', { budQuick: { ai: {
      dictatedPrice: 'Precio dictado',
      approximatePrice: 'Precio aproximado',
      catalogPrice: 'En tu catálogo',
      priceIncludedInPreviousLine: 'Precio incluido en la línea anterior',
    } } });
    translate.use('es');
    fixture = TestBed.createComponent(PresupuestoIaPriceInfoComponent);
  });

  it('shows an icon and dictated-price label plus a different catalogue reference', () => {
    fixture.componentInstance.priceOrigin = 'dictado';
    fixture.componentInstance.approximate = false;
    fixture.componentInstance.catalogPrice = 39;
    fixture.componentInstance.currentPrice = 25;
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Precio dictado');
    expect(fixture.nativeElement.querySelector('.price-badge mat-icon')?.textContent.trim())
      .toBe('record_voice_over');
    expect(fixture.nativeElement.textContent).toContain('En tu catálogo');
    expect(fixture.nativeElement.textContent).toContain('39.00');
  });

  it('shows the approximate label and icon when the dictated amount is approximate', () => {
    fixture.componentInstance.priceOrigin = 'dictado';
    fixture.componentInstance.approximate = true;
    fixture.componentInstance.currentPrice = 25;
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Precio aproximado');
    expect(fixture.nativeElement.querySelector('.price-badge mat-icon')?.textContent.trim()).toBe('functions');
  });

  it('shows the catalogue reference for any changed price without a dictated-price label', () => {
    fixture.componentInstance.priceOrigin = 'catalogo';
    fixture.componentInstance.catalogPrice = 39;
    fixture.componentInstance.currentPrice = 25;
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('En tu catálogo');
    expect(fixture.nativeElement.textContent).not.toContain('Precio dictado');
  });

  it('explains when a line is covered by the total on the previous line', () => {
    fixture.componentInstance.includedInPreviousLine = true;
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Precio incluido en la línea anterior');
    expect(fixture.nativeElement.querySelector('.previous-line-note mat-icon')?.textContent.trim()).toBe('info');
  });
});
