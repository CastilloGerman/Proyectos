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
    const header = banner.querySelector('.hint-header') as HTMLButtonElement;

    expect(header.getAttribute('aria-expanded')).toBe('true');
    expect(banner.querySelector('.hint-body')).not.toBeNull();
    expect(banner.textContent).toContain('Existing banner title');
    expect(banner.textContent).toContain('Existing banner step');
    expect(banner.querySelector('.hint-note')).toBeNull();

    header.click();
    fixture.detectChanges();

    expect(banner.querySelector('.hint-body')).toBeNull();
    expect(banner.textContent).toContain('Existing banner title');
    expect(banner.textContent).not.toContain('Existing banner step');
    expect(header.getAttribute('aria-expanded')).toBe('false');
    expect(localStorage.getItem('hint-existing-banner-test')).toBe('collapsed');
  });

  it('starts collapsed when a previous dismissal is stored', () => {
    localStorage.setItem('hint-dismissed-banner-test', 'dismissed');
    const dismissed = TestBed.createComponent(HintBannerComponent);
    dismissed.componentInstance.storageKey = 'hint-dismissed-banner-test';
    dismissed.componentInstance.title = 'Dismissed banner title';
    dismissed.componentInstance.steps = [{ icon: 'person', text: 'Dismissed banner step' }];
    dismissed.detectChanges();

    const banner = dismissed.nativeElement.querySelector('.hint-banner') as HTMLElement;
    expect(banner.querySelector('.hint-body')).toBeNull();
    expect(banner.querySelector('.hint-header')?.getAttribute('aria-expanded')).toBe('false');
    expect(banner.textContent).toContain('Dismissed banner title');
    expect(banner.textContent).not.toContain('Dismissed banner step');
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
