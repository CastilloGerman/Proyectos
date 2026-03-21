import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

/**
 * Placeholder de Historial de accesos hasta implementar registro de logins en backend.
 */
@Component({
  selector: 'app-historial-accesos',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, MatIconModule, RouterLink],
  templateUrl: './historial-accesos.component.html',
  styleUrl: './historial-accesos.component.scss',
})
export class HistorialAccesosComponent {}
