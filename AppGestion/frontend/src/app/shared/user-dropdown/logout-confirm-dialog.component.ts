import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';

/**
 * Confirmación antes de cerrar sesión (solo borra token en este dispositivo).
 * No expone el JWT en UI; la acción delega en AuthService.logout().
 */
@Component({
    selector: 'app-logout-confirm-dialog',
    imports: [MatDialogModule, MatButtonModule],
    template: `
    <h2 mat-dialog-title>Cerrar sesión</h2>
    <mat-dialog-content class="dlg-body">
      <p>¿Seguro que quieres salir? Tendrás que volver a iniciar sesión para acceder a tus datos.</p>
      <p class="hint">La sesión se cierra en este navegador o dispositivo.</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" mat-dialog-close="cancel">Cancelar</button>
      <button mat-flat-button type="button" color="warn" [mat-dialog-close]="true">Cerrar sesión</button>
    </mat-dialog-actions>
  `,
    styles: `
    .dlg-body {
      padding-top: 0;
      max-width: 360px;
    }
    .hint {
      font-size: 13px;
      color: var(--app-text-secondary, #64748b);
      margin-bottom: 0;
    }
  `
})
export class LogoutConfirmDialogComponent {}
