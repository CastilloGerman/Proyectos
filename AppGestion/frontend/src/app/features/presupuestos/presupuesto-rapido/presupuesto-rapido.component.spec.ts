import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { PresupuestoRapidoComponent } from './presupuesto-rapido.component';
import { PresupuestoService } from '../../../core/services/presupuesto.service';
import { ClienteService } from '../../../core/services/cliente.service';
import { MaterialService } from '../../../core/services/material.service';
import { AuthService } from '../../../core/auth/auth.service';
import { MatSnackBar } from '@angular/material/snack-bar';

describe('PresupuestoRapidoComponent adjuntos', () => {
  let fixture: ComponentFixture<PresupuestoRapidoComponent>;
  let component: PresupuestoRapidoComponent;
  const presupuestoService = {
    create: vi.fn(),
    downloadPdf: vi.fn(),
    uploadFoto: vi.fn(),
    uploadFirma: vi.fn(),
  };
  const snackBar = { open: vi.fn() };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        PresupuestoRapidoComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideRouter([]),
        { provide: PresupuestoService, useValue: presupuestoService },
        { provide: ClienteService, useValue: { getAll: () => of([]), createProvisional: vi.fn() } },
        { provide: MaterialService, useValue: { getAll: () => of([]) } },
        { provide: AuthService, useValue: { canMutate: () => true } },
        { provide: MatSnackBar, useValue: snackBar },
      ],
    }).compileComponents();

    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('es', {
      snack: {
        budgetPhotoOk: 'Foto del trabajo guardada.',
        budgetPhotoFail: 'No pudimos guardar la foto. Inténtalo de nuevo.',
        budgetSignOk: 'Firma del cliente guardada.',
        budgetSignFail: 'No pudimos guardar la firma. Inténtalo de nuevo.',
      },
      common: { close: 'Cerrar', cancel: 'Cancelar' },
    });
    translate.setFallbackLang('es');
    translate.use('es');

    fixture = TestBed.createComponent(PresupuestoRapidoComponent);
    component = fixture.componentInstance;
    presupuestoService.uploadFirma.mockReset();
    snackBar.open.mockReset();
    fixture.detectChanges();
  });

  it('uploads a signature after the budget is saved', () => {
    component.savedPresupuestoId = 12;
    presupuestoService.uploadFirma.mockReturnValue(of(undefined));

    component.onFirmaCapturada('data:image/png;base64,QQ==');

    expect(presupuestoService.uploadFirma).toHaveBeenCalled();
    expect(component.firmaOk).toBe(true);
    expect(component.subiendoFirma).toBe(false);
  });

  it('shows an error when the signature upload fails', () => {
    component.savedPresupuestoId = 12;
    presupuestoService.uploadFirma.mockReturnValue(throwError(() => ({ status: 500 })));

    component.onFirmaCapturada('data:image/png;base64,QQ==');

    expect(presupuestoService.uploadFirma).toHaveBeenCalled();
    expect(component.firmaOk).toBe(false);
    expect(component.subiendoFirma).toBe(false);
  });
});
