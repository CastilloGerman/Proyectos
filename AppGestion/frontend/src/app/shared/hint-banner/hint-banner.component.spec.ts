import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { HintBannerComponent } from './hint-banner.component';

describe('HintBannerComponent', () => {
  let fixture: ComponentFixture<HintBannerComponent>;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({ imports: [HintBannerComponent, TranslateModule.forRoot()] }).compileComponents();
    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('es', { 'hint.translatedStep': 'Translated hint step' });
    translate.use('es');
    fixture = TestBed.createComponent(HintBannerComponent);
    fixture.componentInstance.storageKey = 'hint-existing-banner-test';
    fixture.componentInstance.title = 'Existing banner title';
    fixture.componentInstance.steps = [{ icon: 'person', text: 'Existing banner step' }];
    fixture.detectChanges();
  });

  afterEach(() => localStorage.clear());

  it('keeps existing banners unchanged when the optional note is omitted', () => {
    const banner = fixture.nativeElement.querySelector('.hint-banner') as HTMLElement;
    const closeButton = banner.querySelector('.hint-close') as HTMLButtonElement;

    expect(fixture.componentInstance.visible).toBe(true);
    expect(banner.textContent).toContain('Existing banner title');
    expect(banner.textContent).toContain('Existing banner step');
    expect(banner.querySelector('.hint-note')).toBeNull();
    expect(getComputedStyle(closeButton).width).toBe('28px');

    closeButton.click();
    fixture.detectChanges();
    expect(fixture.componentInstance.visible).toBe(false);
    expect(localStorage.getItem('hint-existing-banner-test')).toBe('dismissed');
  });

  it('renders the optional note only when a banner provides one', () => {
    fixture.componentInstance.note = 'Optional note';
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.hint-note')?.textContent).toContain('Optional note');
  });

  it('keeps translated step keys supported alongside plain text steps', () => {
    fixture.componentInstance.steps = [{ icon: 'person', text: '', textKey: 'hint.translatedStep' }];
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.step-text')?.textContent).toContain('Translated hint step');
  });
});
