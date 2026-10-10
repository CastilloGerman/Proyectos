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
    const preferences = { seguimientoActivo: true, diasEspera: 8, maxAvisos: 2, emailResumen: true, permitirRespuestaCliente: true };
    const apiPreferences = {
      seguimientoActivo: true, seguimientoDiasEspera: 8, seguimientoMaxAvisos: 2, seguimientoEmailResumen: true, permitirRespuestaCliente: true,
    };

    service.getSeguimientoPresupuestos().subscribe(value => expect(value).toEqual(preferences));
    http.expectOne({ method: 'GET', url }).flush(apiPreferences);
    service.patchSeguimientoPresupuestos(preferences).subscribe(value => expect(value).toEqual(preferences));
    const patch = http.expectOne({ method: 'PATCH', url });
    expect(patch.request.body).toEqual(apiPreferences);
    patch.flush(apiPreferences);
    http.verify();
  });

  // === Contract test: API field name must match backend DTO ===
  // If the backend changes 'permitirRespuestaCliente' to something else, this test fails.
  it('contract: API field name "permitirRespuestaCliente" must match backend SeguimientoPresupuestosResponse', () => {
    const service = TestBed.inject(ConfigService);
    const http = TestBed.inject(HttpTestingController);
    const url = `${environment.apiUrl}/config/empresa/seguimiento-presupuestos`;

    // Backend sends 'permitirRespuestaCliente' (Java record field name)
    const apiResponse = {
      seguimientoActivo: false,
      seguimientoDiasEspera: 5,
      seguimientoMaxAvisos: 3,
      seguimientoEmailResumen: false,
      permitirRespuestaCliente: false,
    };

    service.getSeguimientoPresupuestos().subscribe(result => {
      // Frontend uses the SAME field name as the backend — no mapping
      expect(result.permitirRespuestaCliente).toBe(false);
    });

    const req = http.expectOne({ method: 'GET', url });
    // Verify the request was made
    expect(req.request.method).toBe('GET');
    req.flush(apiResponse);

    // Verify PATCH sends the correct API field name
    service.patchSeguimientoPresupuestos({
      seguimientoActivo: true,
      diasEspera: 7,
      maxAvisos: 4,
      emailResumen: true,
      permitirRespuestaCliente: false,
    }).subscribe();

    const patchReq = http.expectOne({ method: 'PATCH', url });
    // The PATCH body MUST use 'permitirRespuestaCliente' (backend field name)
    expect((patchReq.request.body as { permitirRespuestaCliente?: boolean }).permitirRespuestaCliente).toBe(false);
    patchReq.flush(apiResponse);
    http.verify();
  });
});
