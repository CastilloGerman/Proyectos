import { describe, it, expect, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { ConfigService } from './config.service';

describe('ConfigService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
  });

  it('should create', () => {
    expect(TestBed.inject(ConfigService)).toBeTruthy();
  });

  it('loads and patches personal budget follow-up preferences', () => {
    const service = TestBed.inject(ConfigService);
    const http = TestBed.inject(HttpTestingController);
    const url = `${environment.apiUrl}/config/empresa/seguimiento-presupuestos`;
    const preferences = { seguimientoActivo: true, diasEspera: 8, maxAvisos: 2, emailResumen: true };
    const apiPreferences = {
      seguimientoActivo: true, seguimientoDiasEspera: 8, seguimientoMaxAvisos: 2, seguimientoEmailResumen: true,
    };

    service.getSeguimientoPresupuestos().subscribe(value => expect(value).toEqual(preferences));
    http.expectOne({ method: 'GET', url }).flush(apiPreferences);
    service.patchSeguimientoPresupuestos(preferences).subscribe(value => expect(value).toEqual(preferences));
    const patch = http.expectOne({ method: 'PATCH', url });
    expect(patch.request.body).toEqual(apiPreferences);
    patch.flush(apiPreferences);
    http.verify();
  });
});
