import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PresupuestoService } from './presupuesto.service';
import { environment } from '../../../environments/environment';

const API = `${environment.apiUrl}/presupuestos`;

describe('PresupuestoService', () => {
  let service: PresupuestoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(PresupuestoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('should create', () => {
    expect(service).toBeTruthy();
  });

  describe('getAll', () => {
    it('GETs /presupuestos without params by default', () => {
      service.getAll().subscribe((data) => expect(data.length).toBe(1));

      const req = http.expectOne(API);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.keys().length).toBe(0);
      req.flush([{ id: 1, total: 500 }]);
    });

    it('passes search query param', () => {
      service.getAll('obra').subscribe();

      const req = http.expectOne((r) => r.url === API);
      expect(req.request.params.get('q')).toBe('obra');
      req.flush([]);
    });
  });

  describe('create', () => {
    it('POSTs PresupuestoRequest to /presupuestos', () => {
      const body = {
        clienteId: 2,
        items: [{ cantidad: 3, precioUnitario: 50, aplicaIva: true }],
        ivaHabilitado: true,
        descuentoGlobalPorcentaje: 0,
      };
      service.create(body).subscribe((p) => expect(p.id).toBe(99));

      const req = http.expectOne(API);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(body);
      req.flush({ id: 99, total: 181.5, items: [] });
    });
  });

  describe('createFacturaFromPresupuesto', () => {
    it('POSTs to /presupuestos/:id/factura', () => {
      service.createFacturaFromPresupuesto(12).subscribe((f) => expect(f.id).toBe(50));

      const req = http.expectOne(`${API}/12/factura`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      req.flush({ id: 50, presupuestoId: 12, items: [] });
    });
  });

  describe('createFacturaFinalFromPresupuesto', () => {
    it('POSTs to /presupuestos/:id/factura-final', () => {
      service.createFacturaFinalFromPresupuesto(12).subscribe();

      const req = http.expectOne(`${API}/12/factura-final`);
      expect(req.request.method).toBe('POST');
      req.flush({ id: 51, tipoFactura: 'FINAL_CON_ANTICIPO', items: [] });
    });
  });

  describe('registrarAnticipo', () => {
    it('POSTs anticipo to /presupuestos/:id/anticipo', () => {
      const body = { importeAnticipo: 300, fechaAnticipo: '2026-03-01' };
      service.registrarAnticipo(5, body).subscribe();

      const req = http.expectOne(`${API}/5/anticipo`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(body);
      req.flush({ id: 5, tieneAnticipo: true, importeAnticipo: 300, items: [] });
    });
  });

  describe('getResumenAnticipo', () => {
    it('GETs /presupuestos/:id/resumen-anticipo', () => {
      service.getResumenAnticipo(5).subscribe((r) => {
        expect(r.importePendiente).toBe(700);
      });

      const req = http.expectOne(`${API}/5/resumen-anticipo`);
      expect(req.request.method).toBe('GET');
      req.flush({
        totalPresupuesto: 1000,
        importeAnticipo: 300,
        baseAnticipo: 247.93,
        ivaAnticipo: 52.07,
        importePendiente: 700,
        basePendiente: 578.51,
        ivaPendiente: 121.49,
        anticipoYaFacturado: false,
        tieneAnticipoRegistrado: true,
      });
    });
  });

  describe('getCondicionesDisponibles', () => {
    it('GETs catalog from /presupuestos/condiciones-disponibles', () => {
      service.getCondicionesDisponibles().subscribe((list) => {
        expect(list[0].clave).toBe('validez_30');
      });

      const req = http.expectOne(`${API}/condiciones-disponibles`);
      expect(req.request.method).toBe('GET');
      req.flush([{ clave: 'validez_30', texto: 'Validez 30 días' }]);
    });
  });

  describe('delete', () => {
    it('DELETEs /presupuestos/:id', () => {
      service.delete(9).subscribe();

      const req = http.expectOne(`${API}/9`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('adjuntos', () => {
    it('POSTs multipart foto to /presupuestos/:id/foto', () => {
      const blob = new Blob(['x'], { type: 'image/jpeg' });
      service.uploadFoto(8, blob, 'foto.jpg').subscribe();

      const req = http.expectOne(`${API}/8/foto`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toBeInstanceOf(FormData);
      req.flush(null);
    });

    it('POSTs multipart firma to /presupuestos/:id/firma', () => {
      const blob = new Blob(['y'], { type: 'image/png' });
      service.uploadFirma(8, blob).subscribe();

      const req = http.expectOne(`${API}/8/firma`);
      expect(req.request.method).toBe('POST');
      req.flush(null);
    });

    it('GETs foto blob', () => {
      service.downloadFoto(8).subscribe((b) => expect(b.size).toBeGreaterThan(0));
      const req = http.expectOne(`${API}/8/foto`);
      expect(req.request.method).toBe('GET');
      req.flush(new Blob(['img']));
    });
  });
});
