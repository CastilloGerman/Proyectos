import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import * as fs from 'node:fs';
import * as path from 'node:path';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AboutComponent } from './about.component';

class MockIntersectionObserver {
  observe = vi.fn();
  unobserve = vi.fn();
  disconnect = vi.fn();
}

describe('AboutComponent', () => {
  let fixture: ComponentFixture<AboutComponent>;

  beforeEach(async () => {
    vi.stubGlobal('IntersectionObserver', MockIntersectionObserver);

    await TestBed.configureTestingModule({
      imports: [AboutComponent, TranslateModule.forRoot()],
      providers: [provideRouter([])],
    }).compileComponents();

    const translate = TestBed.inject(TranslateService);
    const es = JSON.parse(fs.readFileSync(path.join(process.cwd(), 'src', 'assets', 'i18n', 'es.json'), 'utf8'));
    translate.setTranslation('es', es);
    translate.setFallbackLang('es');
    translate.use('es');

    fixture = TestBed.createComponent(AboutComponent);
    fixture.detectChanges();
  });

  it('renders the Spanish about copy instead of raw i18n keys', () => {
    const text = fixture.nativeElement.textContent as string;

    expect(text).toContain('Nuestra historia');
    expect(text).toContain('Velocidad en campo');
    expect(text).toContain('Querido compañero');
    expect(text).not.toContain('auth.about.');
  });
});
