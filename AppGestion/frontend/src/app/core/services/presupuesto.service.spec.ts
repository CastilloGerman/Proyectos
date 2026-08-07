import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PresupuestoService } from './presupuesto.service';

describe('PresupuestoService', () => {
  let service: PresupuestoService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(PresupuestoService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(service).toBeTruthy();
  });

  it('updates only estado with PATCH', () => {
    service.updateEstado(42, 'Aceptado').subscribe((res) => {
      expect(res.estado).toBe('Aceptado');
    });

    const req = httpMock.expectOne((request) => request.url.endsWith('/presupuestos/42/estado'));
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ estado: 'Aceptado' });

    req.flush({
      id: 42,
      clienteId: 7,
      clienteNombre: 'Cliente',
      fechaCreacion: '2026-06-22T10:00:00',
      subtotal: 90,
      iva: 0,
      total: 90,
      ivaHabilitado: true,
      estado: 'Aceptado',
      items: [],
    });
  });
});
