import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PresupuestoIaService } from './presupuesto-ia.service';
import { PresupuestoIaRequestError } from '../models/presupuesto-ia.model';
import { environment } from '../../../environments/environment';

const API = `${environment.apiUrl}/presupuestos/ia/borrador`;

describe('PresupuestoIaService', () => {
  let service: PresupuestoIaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(PresupuestoIaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('posts text and returns the backend draft contract without persisting it', () => {
    const expected = {
      clienteId: null,
      clienteNombre: 'Obra de prueba',
      clienteTelefono: null,
      transcripcion: 'alicatar seis metros',
      items: [{
        materialId: 8, materialNombre: 'Azulejo mate', tareaManual: 'Alicatar baño', cantidad: 6,
        precioUnitario: 22, unidad: 'm2', aplicaIva: true, descuentoPorcentaje: 0, descuentoFijo: 0,
        visiblePdf: true, confianza: 'media', faltaPrecio: false, cantidadDudosa: false,
      }],
      notaAdicional: null,
    };
    service.generarBorrador({ texto: 'alicatar seis metros', clienteId: 4 }).subscribe((draft) => {
      expect(draft).toEqual(expected);
    });
    const req = http.expectOne(API);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ texto: 'alicatar seis metros', clienteId: 4 });
    req.flush(expected);
  });

  it.each([
    [503, 'El servicio de IA está desactivado', 'disabled'],
    [429, 'Has alcanzado el límite diario de borradores de presupuesto con IA. Inténtalo mañana.', 'quota-daily'],
    [429, 'Has alcanzado el límite de solicitudes de IA por hora.', 'quota-hourly'],
    [429, 'Has alcanzado el límite de intentos del servicio de IA por hora.', 'quota-attempts'],
    [429, 'Has alcanzado el límite de cuota del servicio de IA.', 'quota-provider'],
    [503, 'Servicio de IA no disponible, inténtalo en unos minutos', 'provider'],
    [502, 'La respuesta del servicio de IA no tiene un formato válido', 'provider'],
    [400, 'La descripción no puede superar los 8000 caracteres.', 'too-long'],
    [403, 'Tu plan actual no permite crear presupuestos.', 'forbidden'],
    [401, 'Inicia sesión para generar un presupuesto.', 'unauthorized'],
    [400, 'Describe el trabajo que hay que presupuestar.', 'invalid-request'],
    [404, 'Cliente no encontrado.', 'unknown'],
  ] as const)('maps HTTP %s to %s', (status, message, kind) => {
    let actual: unknown;
    service.generarBorrador({ texto: 'trabajo' }).subscribe({ error: (error) => actual = error });
    http.expectOne(API).flush({ message, detail: message, status }, { status, statusText: 'Error' });
    expect(actual).toBeInstanceOf(PresupuestoIaRequestError);
    expect((actual as PresupuestoIaRequestError).kind).toBe(kind);
    expect((actual as PresupuestoIaRequestError).serverMessage).toBe(message);
  });
});
