import { CommonModule } from '@angular/common';
import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  OnDestroy,
  Output,
  ViewChild,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-signature-pad',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, TranslateModule],
  template: `
    <div class="sig-wrap">
      <canvas
        #canvas
        class="sig-canvas"
        (pointerdown)="onPointerDown($event)"
        (pointermove)="onPointerMove($event)"
        (pointerup)="onPointerUp($event)"
        (pointercancel)="onPointerUp($event)"
        (touchstart)="onTouchGuard($event)"
        (touchmove)="onTouchGuard($event)"
        (touchend)="onTouchGuard($event)"
      ></canvas>
      <div class="sig-actions">
        <button
          mat-stroked-button
          type="button"
          class="sig-btn"
          (click)="clear()"
          [attr.aria-label]="'signature.clear' | translate"
        >
          <mat-icon>backspace</mat-icon>
          {{ 'signature.clear' | translate }}
        </button>
        <button
          mat-raised-button
          color="primary"
          type="button"
          class="sig-btn"
          (click)="confirm()"
          [disabled]="!hasStroke"
        >
          <mat-icon>check</mat-icon>
          {{ 'signature.confirm' | translate }}
        </button>
      </div>
    </div>
  `,
  styles: [`
    .sig-wrap {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .sig-canvas {
      display: block;
      width: 100%;
      height: 220px;
      touch-action: none;
      border-radius: 12px;
      border: 1px solid var(--app-border, rgba(15, 23, 42, 0.12));
      background: #ffffff;
      cursor: crosshair;
    }
    :host-context(html.app-dark-theme) .sig-canvas {
      background: #1a222c;
      border-color: rgba(255, 255, 255, 0.12);
    }
    .sig-actions {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
    }
    .sig-btn {
      min-height: 44px;
      min-width: 44px;
    }
  `],
})
export class SignaturePadComponent implements AfterViewInit, OnDestroy {
  @ViewChild('canvas', { static: true }) canvasRef!: ElementRef<HTMLCanvasElement>;
  @Output() signatureCaptured = new EventEmitter<string>();

  hasStroke = false;

  private ctx: CanvasRenderingContext2D | null = null;
  private drawing = false;
  private resizeObserver?: ResizeObserver;

  ngAfterViewInit(): void {
    const canvas = this.canvasRef.nativeElement;
    this.ctx = canvas.getContext('2d');
    this.fitCanvas();
    this.paintBackground();
    if (typeof ResizeObserver !== 'undefined') {
      this.resizeObserver = new ResizeObserver(() => this.fitCanvas(true));
      this.resizeObserver.observe(canvas);
    }
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
  }

  @HostListener('window:resize')
  onWindowResize(): void {
    this.fitCanvas(true);
  }

  onTouchGuard(event: TouchEvent): void {
    event.preventDefault();
  }

  onPointerDown(event: PointerEvent): void {
    if (!this.ctx) return;
    event.preventDefault();
    const canvas = this.canvasRef.nativeElement;
    if (typeof canvas.setPointerCapture === 'function') {
      try {
        canvas.setPointerCapture(event.pointerId);
      } catch {
        /* jsdom / pointer already captured */
      }
    }
    this.drawing = true;
    const p = this.point(event);
    this.ctx.beginPath();
    this.ctx.moveTo(p.x, p.y);
    this.applyStrokeStyle();
  }

  onPointerMove(event: PointerEvent): void {
    if (!this.drawing || !this.ctx) return;
    event.preventDefault();
    const p = this.point(event);
    this.ctx.lineTo(p.x, p.y);
    this.ctx.stroke();
    this.hasStroke = true;
  }

  onPointerUp(event: PointerEvent): void {
    if (!this.drawing) return;
    event.preventDefault();
    this.drawing = false;
    try {
      this.canvasRef.nativeElement.releasePointerCapture(event.pointerId);
    } catch {
      /* pointer already released */
    }
  }

  clear(): void {
    this.hasStroke = false;
    this.paintBackground();
  }

  confirm(): void {
    if (!this.hasStroke) return;
    const dataUrl = this.canvasRef.nativeElement.toDataURL('image/png');
    this.signatureCaptured.emit(dataUrl);
  }

  private fitCanvas(preserve = false): void {
    const canvas = this.canvasRef.nativeElement;
    const snapshot = preserve && this.hasStroke ? canvas.toDataURL('image/png') : null;
    const ratio = typeof window !== 'undefined' ? window.devicePixelRatio || 1 : 1;
    const cssWidth = canvas.clientWidth || 320;
    const cssHeight = canvas.clientHeight || 220;
    canvas.width = Math.max(1, Math.round(cssWidth * ratio));
    canvas.height = Math.max(1, Math.round(cssHeight * ratio));
    this.ctx = canvas.getContext('2d');
    this.ctx?.setTransform(ratio, 0, 0, ratio, 0, 0);
    this.paintBackground();
    if (snapshot && this.ctx) {
      const img = new Image();
      img.onload = () => {
        this.ctx?.drawImage(img, 0, 0, cssWidth, cssHeight);
      };
      img.src = snapshot;
    }
  }

  private paintBackground(): void {
    if (!this.ctx) return;
    const canvas = this.canvasRef.nativeElement;
    const cssWidth = canvas.clientWidth || canvas.width;
    const cssHeight = canvas.clientHeight || canvas.height;
    this.ctx.fillStyle = this.isDark() ? '#1a222c' : '#ffffff';
    this.ctx.fillRect(0, 0, cssWidth, cssHeight);
  }

  private applyStrokeStyle(): void {
    if (!this.ctx) return;
    this.ctx.strokeStyle = this.isDark() ? '#f8fafc' : '#0f172a';
    this.ctx.lineWidth = 2.4;
    this.ctx.lineCap = 'round';
    this.ctx.lineJoin = 'round';
  }

  private isDark(): boolean {
    return typeof document !== 'undefined'
      && document.documentElement.classList.contains('app-dark-theme');
  }

  private point(event: PointerEvent): { x: number; y: number } {
    const rect = this.canvasRef.nativeElement.getBoundingClientRect();
    return { x: event.clientX - rect.left, y: event.clientY - rect.top };
  }
}
