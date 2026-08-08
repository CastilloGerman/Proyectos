import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { FacturaService } from './factura.service';
import { environment } from '../../../environments/environment';

const API = `${environment.apiUrl}/facturas`;

describe('FacturaService', () => {
  let service: FacturaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(FacturaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('should create', () => {
    expect(service).toBeTruthy();
  });

  describe('getAll', () => {
    it('GETs /facturas without params by default', () => {
      service.getAll().subscribe((data) => expect(data.length).toBe(1));

      const req = http.expectOne(API);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.keys().length).toBe(0);
      req.flush([{ id: 1, numeroFactura: 'F-001' }]);
    });

    it('passes search and incluirAnuladas query params', () => {
      service.getAll('cliente', true).subscribe();

      const req = http.expectOne((r) => r.url === API);
      expect(req.request.params.get('q')).toBe('cliente');
      expect(req.request.params.get('incluirAnuladas')).toBe('true');
      req.flush([]);
    });
  });

  describe('getById', () => {
    it('GETs /facturas/:id', () => {
      service.getById(42).subscribe((f) => expect(f.id).toBe(42));

      const req = http.expectOne(`${API}/42`);
      expect(req.request.method).toBe('GET');
      req.flush({ id: 42, numeroFactura: 'F-042', items: [] });
    });
  });

  describe('create', () => {
    it('POSTs FacturaRequest to /facturas', () => {
      const body = {
        clienteId: 1,
        fechaExpedicion: '2026-01-15',
        items: [{ cantidad: 1, precioUnitario: 100, aplicaIva: true }],
        ivaHabilitado: true,
      };
      service.create(body).subscribe((f) => expect(f.total).toBe(121));

      const req = http.expectOne(API);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(body);
      req.flush({ id: 10, total: 121, items: [] });
    });
  });

  describe('registrarCobro', () => {
    it('POSTs cobro to /facturas/:id/cobros', () => {
      const cobro = { importe: 250, fecha: '2026-02-01', metodo: 'Transferencia' };
      service.registrarCobro(5, cobro).subscribe();

      const req = http.expectOne(`${API}/5/cobros`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(cobro);
      req.flush({ id: 5, estadoPago: 'Parcial', montoCobrado: 250, items: [] });
    });
  });

  describe('anular', () => {
    it('POSTs motivo to /facturas/:id/anular', () => {
      service.anular(7, 'Error en datos').subscribe();

      const req = http.expectOne(`${API}/7/anular`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ motivo: 'Error en datos' });
      req.flush(null);
    });

    it('sends null motivo when omitted', () => {
      service.anular(7).subscribe();

      const req = http.expectOne(`${API}/7/anular`);
      expect(req.request.body).toEqual({ motivo: null });
      req.flush(null);
    });
  });

  describe('downloadPdf', () => {
    it('GETs PDF blob from /facturas/:id/pdf', () => {
      service.downloadPdf(3).subscribe((blob) => expect(blob.type).toBe('application/pdf'));

      const req = http.expectOne(`${API}/3/pdf`);
      expect(req.request.method).toBe('GET');
      expect(req.request.responseType).toBe('blob');
      req.flush(new Blob(['pdf'], { type: 'application/pdf' }));
    });
  });

  describe('generarEnlacePago', () => {
    it('POSTs to /facturas/:id/payment-link', () => {
      service.generarEnlacePago(8).subscribe((f) => expect(f.paymentLinkUrl).toBeTruthy());

      const req = http.expectOne(`${API}/8/payment-link`);
      expect(req.request.method).toBe('POST');
      req.flush({ id: 8, paymentLinkUrl: 'https://pay.example/8', items: [] });
    });
  });
});
