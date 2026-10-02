import { TestBed } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { of } from 'rxjs';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ClienteService } from '../../../core/services/cliente.service';
import { PresupuestoService } from '../../../core/services/presupuesto.service';
import { AuthService } from '../../../core/auth/auth.service';
import { construirMensajePresupuesto, EnviarPresupuestoComponent, normalizarTelefonoInternacional } from './enviar-presupuesto.component';

describe('EnviarPresupuestoComponent', () => {
  afterEach(() => vi.restoreAllMocks());

  it('normaliza teléfono español y usa prefijo internacional del país', () => {
    expect(normalizarTelefonoInternacional('600 111 222')).toBe('34600111222');
    expect(normalizarTelefonoInternacional('06 12 34 56 78', 'FR')).toBe('33612345678');
    expect(normalizarTelefonoInternacional('+44 7700 900123', 'GB')).toBe('447700900123');
  });

  it('construye el mensaje con resumen, total y contratista', () => {
    const msg = construirMensajePresupuesto({
      id: 7, clienteId: 2, clienteNombre: 'Ana', fechaCreacion: '', subtotal: 100,
      iva: 21, total: 121, ivaHabilitado: true, estado: 'Pendiente',
      items: [{ descripcion: 'Alicatado', cantidad: 2, precioUnitario: 50 }],
    }, 'Reformas Sol');
    expect(msg).toContain('Alicatado');
    expect(msg).toContain('121.00 €');
    expect(msg).toContain('Reformas Sol');
  });

  it('comparte el PDF con navigator.share y registra WhatsApp al completarse', async () => {
    const share = vi.fn().mockResolvedValue(undefined);
    vi.stubGlobal('navigator', { share, canShare: () => true, clipboard: { writeText: vi.fn() } });
    const presupuestoApi = {
      downloadPdf: vi.fn(() => of(new Blob(['pdf']))),
      marcarEnviado: vi.fn(() => of(void 0)),
    };
    TestBed.configureTestingModule({
      imports: [EnviarPresupuestoComponent, TranslateModule.forRoot()],
      providers: [
        { provide: PresupuestoService, useValue: presupuestoApi },
        { provide: ClienteService, useValue: {} },
        { provide: AuthService, useValue: { user: () => ({ nombre: 'Empresa' }) } },
      ],
    });
    const component = TestBed.createComponent(EnviarPresupuestoComponent).componentInstance;
    component.presupuesto = {
      id: 1, clienteId: 2, clienteNombre: 'Ana', clienteTelefono: '600111222', fechaCreacion: '',
      subtotal: 10, iva: 2.1, total: 12.1, ivaHabilitado: true, estado: 'Pendiente', items: [],
    };
    component.ngOnChanges();
    await component.compartirWhatsApp();
    await Promise.resolve();
    expect(share).toHaveBeenCalled();
    expect(presupuestoApi.marcarEnviado).toHaveBeenCalledWith(1, 'WHATSAPP');
  });
});
