import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslateModule } from '@ngx-translate/core';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it } from 'vitest';
import { AuthService } from '../../../core/auth/auth.service';
import { PresupuestoService } from '../../../core/services/presupuesto.service';
import { PresupuestoListComponent } from './presupuesto-list.component';

describe('PresupuestoListComponent AI create action', () => {
  let fixture: ComponentFixture<PresupuestoListComponent>;
  let canMutate: boolean;

  beforeEach(async () => {
    canMutate = false;
    await TestBed.configureTestingModule({
      imports: [PresupuestoListComponent, NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { queryParams: of({}), snapshot: { queryParamMap: { get: () => null } } } },
        { provide: AuthService, useValue: { canMutate: () => canMutate } },
        { provide: PresupuestoService, useValue: { getAll: () => of([]) } },
        { provide: MatDialog, useValue: { open: () => ({ afterClosed: () => of(null) }) } },
        { provide: MatSnackBar, useValue: { open: () => undefined } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PresupuestoListComponent);
    fixture.detectChanges();
  });

  it('hides Create with AI when the user cannot write', () => {
    expect(fixture.nativeElement.querySelector('a[href*="ia=1"]')).toBeNull();
    expect(fixture.nativeElement.textContent).not.toContain('budList.createWithAi');
  });

  it('links Create with AI to a new form with the AI panel focus query parameter', () => {
    canMutate = true;
    fixture.detectChanges();

    const aiLink = fixture.nativeElement.querySelector('a[href*="ia=1"]') as HTMLAnchorElement;
    expect(aiLink).not.toBeNull();
    expect(aiLink.getAttribute('href')).toContain('/presupuestos/nuevo');
    expect(aiLink.getAttribute('href')).toContain('ia=1');
  });

  it('shows a subtle follow-up indicator only for budgets with a pending reminder', () => {
    fixture.componentInstance.dataSource.data = [
      { id: 1, clienteNombre: 'Pending', estado: 'Pendiente', seguimientoAvisosEnviados: 1, seguimientoSilenciado: false } as never,
      { id: 2, clienteNombre: 'Clear', estado: 'Pendiente', seguimientoAvisosEnviados: 1, seguimientoSilenciado: true } as never,
      { id: 3, clienteNombre: 'Resolved', estado: 'Aceptado', seguimientoAvisosEnviados: 1, seguimientoSilenciado: false } as never,
    ];
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('.follow-up-indicator')).toHaveLength(1);
  });
});
