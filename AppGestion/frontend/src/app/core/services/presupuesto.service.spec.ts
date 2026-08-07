import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PresupuestoService } from './presupuesto.service';
import { environment } from '../../../environments/environment';

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

  it('updates only presupuesto estado with PATCH', () => {
    service.updateEstado(42, 'Aceptado').subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/presupuestos/42/estado`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ estado: 'Aceptado' });
    req.flush({ id: 42, estado: 'Aceptado', items: [] });
  });
});
