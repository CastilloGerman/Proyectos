import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslateModule } from '@ngx-translate/core';
import { of } from 'rxjs';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ClienteService } from '../../../core/services/cliente.service';
import { PresupuestoService } from '../../../core/services/presupuesto.service';
import { AuthService } from '../../../core/auth/auth.service';
import { construirMensajePresupuesto, construirUrlWhatsApp, EnviarPresupuestoComponent, normalizarTelefonoInternacional } from './enviar-presupuesto.component';

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

  it('renders share buttons without needing an expand flag', () => {
    TestBed.configureTestingModule({
      imports: [EnviarPresupuestoComponent, TranslateModule.forRoot()],
      providers: [
        { provide: PresupuestoService, useValue: {} },
        { provide: ClienteService, useValue: {} },
        { provide: AuthService, useValue: { user: () => ({ nombre: 'Empresa' }) } },
      ],
    });
    const fixture = TestBed.createComponent(EnviarPresupuestoComponent);
    fixture.componentInstance.presupuesto = {
      id: 1, clienteId: 2, clienteNombre: 'Ana', fechaCreacion: '', subtotal: 10, iva: 2.1,
      total: 12.1, ivaHabilitado: true, estado: 'Pendiente', items: [],
    };
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.send-buttons')).toBeTruthy();
  });

  it('abre WhatsApp con el teléfono, el mensaje y el enlace público, sin el selector del sistema', async () => {
    const opened = { opener: window, closed: false, location: { replace: vi.fn() } } as unknown as Window & {
      location: { replace: ReturnType<typeof vi.fn> };
    };
    const open = vi.spyOn(window, 'open').mockReturnValue(opened);
    const presupuestoApi = {
      marcarEnviado: vi.fn(() => of(void 0)),
      crearEnlace: vi.fn(() => of({ url: 'https://example.test/p/token', expiraAt: '2027-01-01T00:00:00Z' })),
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
    expect(open).toHaveBeenCalledOnce();
    expect(open.mock.calls[0][0]).toBe('about:blank');
    expect(opened.location.replace).toHaveBeenCalledWith(expect.stringContaining('https://wa.me/34600111222?text='));
    expect(decodeURIComponent(String(opened.location.replace.mock.calls[0][0]))).toContain('https://example.test/p/token');
    expect(opened.opener).toBeNull();
    expect(presupuestoApi.marcarEnviado).toHaveBeenCalledWith(1, 'WHATSAPP');
  });

  it('builds a click-to-chat URL for WhatsApp', () => {
    expect(construirUrlWhatsApp('34600111222', 'Hola\nhttps://example.test/p/x'))
      .toBe('https://wa.me/34600111222?text=Hola%0Ahttps%3A%2F%2Fexample.test%2Fp%2Fx');
  });

  it('adds the public link to the email body while keeping the existing PDF attachment flow', async () => {
    const sendEmail = vi.fn(() => of(void 0));
    const presupuestoApi = {
      crearEnlace: vi.fn(() => of({ url: 'https://example.test/p/mail-token', expiraAt: '2027-01-01T00:00:00Z' })),
      enviarPorEmail: sendEmail,
      marcarEnviado: vi.fn(() => of(void 0)),
    };
    TestBed.configureTestingModule({
      imports: [EnviarPresupuestoComponent, TranslateModule.forRoot()],
      providers: [
        { provide: PresupuestoService, useValue: presupuestoApi },
        { provide: ClienteService, useValue: {} },
        { provide: AuthService, useValue: { user: () => ({ nombre: 'Empresa' }) } },
        { provide: MatSnackBar, useValue: { open: vi.fn() } },
      ],
    });
    const component = TestBed.createComponent(EnviarPresupuestoComponent).componentInstance;
    component.presupuesto = {
      id: 1, clienteId: 2, clienteNombre: 'Ana', clienteEmail: 'ana@example.test',
      fechaCreacion: '', subtotal: 10, iva: 2.1,
      total: 12.1, ivaHabilitado: true, estado: 'Pendiente', items: [],
    };
    component.ngOnChanges();

    await component.enviarEmail();

    expect(sendEmail).toHaveBeenCalledWith(1, expect.objectContaining({
      mensaje: expect.stringContaining('https://example.test/p/mail-token'),
    }));
  });
});
