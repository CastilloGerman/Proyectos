import { CommonModule } from '@angular/common';
import { Component, Input, OnInit } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Factura } from '../../../core/models/factura.model';
import { FacturaService } from '../../../core/services/factura.service';
import { EnviarEmailDialogComponent } from '../../../shared/enviar-email-dialog/enviar-email-dialog.component';

@Component({
  selector: 'app-enviar-factura',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatDialogModule, MatIconModule, MatSnackBarModule, TranslateModule],
  template: `
    <div class="share-options">
      <button mat-raised-button color="primary" type="button" (click)="compartir()" [disabled]="loading || !pdfReady">
        <mat-icon>share</mat-icon> {{ 'invite.shareTooltip' | translate }}
      </button>
      <button mat-stroked-button type="button" (click)="enviarEmail()" [disabled]="loading">
        <mat-icon>email</mat-icon> {{ 'budgetShare.email' | translate }}
      </button>
      <button mat-stroked-button type="button" (click)="descargar()" [disabled]="loading">
        <mat-icon>picture_as_pdf</mat-icon> {{ 'budgetShare.download' | translate }}
      </button>
    </div>
    @if (loading && !pdfReady) { <p role="status">{{ 'invoiceShare.preparing' | translate }}</p> }
  `,
  styles: [`
    .share-options { display:flex; flex-wrap:wrap; gap:12px; margin:12px 0; }
  `],
})
export class EnviarFacturaComponent implements OnInit {
  @Input({ required: true }) factura!: Factura;
  loading = false;
  private pdfBlob: Blob | null = null;
  get pdfReady(): boolean { return this.pdfBlob !== null; }

  ngOnInit(): void {
    this.loading = true;
    this.facturaService.downloadPdf(this.factura.id).subscribe({
      next: (blob) => { this.pdfBlob = blob; this.loading = false; },
      error: (err) => { this.loading = false; this.mostrarError(err, 'budgetShare.pdfFailed'); },
    });
  }

  constructor(
    private facturaService: FacturaService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private translate: TranslateService,
  ) {}

  compartir(): void {
    if (!this.pdfBlob) {
      this.aviso('budgetShare.pdfFailed');
      return;
    }
    const file = new File([this.pdfBlob], `${this.factura.numeroFactura || 'factura'}.pdf`, { type: 'application/pdf' });
    const data: ShareData = {
      files: [file],
      title: this.factura.numeroFactura,
      text: `${this.translate.instant('factForm.invoiceNo')} ${this.factura.numeroFactura}`,
    };
    if (typeof navigator.share === 'function' && (!navigator.canShare || navigator.canShare(data))) {
      void navigator.share(data).catch((error: unknown) => {
        if ((error as { name?: string })?.name !== 'AbortError') this.aviso('budgetShare.shareFailed');
      });
      return;
    }

    // Open synchronously in the click handler so the browser doesn't block it as a popup.
    const whatsapp = window.open(`https://wa.me/?text=${encodeURIComponent(data.text ?? '')}`, '_blank');
    this.descargarBlob(this.pdfBlob, file.name);
    if (!whatsapp) {
      this.aviso('invoiceShare.popupBlocked');
      return;
    }
    this.aviso('invoiceShare.whatsappReady');
  }

  enviarEmail(): void {
    const ref = this.dialog.open(EnviarEmailDialogComponent, {
      width: '400px',
      data: { titulo: this.translate.instant('invList.sendEmailTooltip'), emailCliente: this.factura.clienteEmail || undefined },
    });
    ref.afterClosed().subscribe((email: string | undefined) => {
      if (email === undefined) return;
      this.loading = true;
      this.facturaService.enviarPorEmail(this.factura.id, email || undefined).subscribe({
        next: () => { this.loading = false; this.aviso('invoiceShare.emailQueued'); },
        error: (err) => { this.loading = false; this.mostrarError(err, 'snack.invoiceEmailFail'); },
      });
    });
  }

  descargar(): void {
    this.loading = true;
    this.facturaService.downloadPdf(this.factura.id).subscribe({
      next: (blob) => { this.loading = false; this.descargarBlob(blob, `${this.factura.numeroFactura || 'factura'}.pdf`); },
      error: () => { this.loading = false; this.aviso('budgetShare.pdfFailed'); },
    });
  }

  private descargarBlob(blob: Blob, name: string): void {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = name;
    link.click();
    URL.revokeObjectURL(url);
  }

  private aviso(key: string): void {
    this.snackBar.open(this.translate.instant(key), this.translate.instant('common.close'), { duration: 3500 });
  }

  private mostrarError(err: { error?: { detail?: unknown; message?: unknown } }, fallbackKey: string): void {
    const raw = err.error?.detail ?? err.error?.message;
    const message = typeof raw === 'string' && raw.trim() ? raw.trim() : this.translate.instant(fallbackKey);
    this.snackBar.open(message, this.translate.instant('common.close'), { duration: 6000 });
  }
}
