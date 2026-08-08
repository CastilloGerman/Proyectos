import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { GastoService } from './gasto.service';
import { environment } from '../../../environments/environment';

describe('GastoService', () => {
  let service: GastoService;
  let httpMock: HttpTestingController;
  const apiUrl = `${environment.apiUrl}/gastos`;

  const sampleRequest = {
    proveedor: 'Proveedor SL',
    concepto: 'Material obra',
    fecha: '2026-02-15',
    baseImponible: 100,
    tipoIva: 21,
    categoria: 'MATERIAL' as const,
  };

  const sampleGasto = { id: 1, ...sampleRequest, cuotaIva: 21 };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(GastoService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(service).toBeTruthy();
  });

  it('getAll requests GET /gastos', () => {
    service.getAll().subscribe((data) => expect(data).toEqual([sampleGasto]));
    const req = httpMock.expectOne(apiUrl);
    expect(req.request.method).toBe('GET');
    req.flush([sampleGasto]);
  });

  it('getById requests GET /gastos/:id', () => {
    service.getById(1).subscribe((data) => expect(data).toEqual(sampleGasto));
    const req = httpMock.expectOne(`${apiUrl}/1`);
    expect(req.request.method).toBe('GET');
    req.flush(sampleGasto);
  });

  it('create posts GastoRequest', () => {
    service.create(sampleRequest).subscribe((data) => expect(data.id).toBe(1));
    const req = httpMock.expectOne(apiUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(sampleRequest);
    req.flush(sampleGasto);
  });

  it('update puts GastoRequest', () => {
    service.update(1, sampleRequest).subscribe((data) => expect(data).toEqual(sampleGasto));
    const req = httpMock.expectOne(`${apiUrl}/1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(sampleRequest);
    req.flush(sampleGasto);
  });

  it('delete requests DELETE /gastos/:id', () => {
    service.delete(1).subscribe();
    const req = httpMock.expectOne(`${apiUrl}/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
