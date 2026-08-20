import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { SignaturePadComponent } from './signature-pad.component';

describe('SignaturePadComponent', () => {
  let fixture: ComponentFixture<SignaturePadComponent>;
  let component: SignaturePadComponent;

  beforeEach(async () => {
    const ctx = {
      beginPath: vi.fn(),
      moveTo: vi.fn(),
      lineTo: vi.fn(),
      stroke: vi.fn(),
      fillRect: vi.fn(),
      setTransform: vi.fn(),
      drawImage: vi.fn(),
      fillStyle: '',
      strokeStyle: '',
      lineWidth: 1,
      lineCap: 'round',
      lineJoin: 'round',
    };
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(ctx as unknown as CanvasRenderingContext2D);

    await TestBed.configureTestingModule({
      imports: [SignaturePadComponent, TranslateModule.forRoot()],
    }).compileComponents();

    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('es', {
      signature: { clear: 'Borrar firma', confirm: 'Confirmar firma' },
    });
    translate.setFallbackLang('es');
    translate.use('es');

    fixture = TestBed.createComponent(SignaturePadComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  function stroke(): void {
    const canvas = fixture.nativeElement.querySelector('canvas') as HTMLCanvasElement;
    canvas.dispatchEvent(new PointerEvent('pointerdown', { clientX: 10, clientY: 10, pointerId: 1, bubbles: true }));
    canvas.dispatchEvent(new PointerEvent('pointermove', { clientX: 40, clientY: 40, pointerId: 1, bubbles: true }));
    canvas.dispatchEvent(new PointerEvent('pointerup', { clientX: 40, clientY: 40, pointerId: 1, bubbles: true }));
  }

  it('draws, clears, and emits a PNG data URL on confirm', () => {
    const emitted: string[] = [];
    component.signatureCaptured.subscribe((url) => emitted.push(url));

    stroke();
    expect(component.hasStroke).toBe(true);

    component.clear();
    expect(component.hasStroke).toBe(false);

    stroke();
    const toDataUrl = vi.spyOn(HTMLCanvasElement.prototype, 'toDataURL').mockReturnValue('data:image/png;base64,AAA');
    component.confirm();

    expect(emitted).toEqual(['data:image/png;base64,AAA']);
    toDataUrl.mockRestore();
  });

  it('does not emit if the canvas is empty', () => {
    const emitted: string[] = [];
    component.signatureCaptured.subscribe((url) => emitted.push(url));
    component.confirm();
    expect(emitted).toEqual([]);
  });
});
