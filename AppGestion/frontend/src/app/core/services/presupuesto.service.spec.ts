import { describe, it, expect, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { PresupuestoService } from './presupuesto.service';

describe('PresupuestoService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
  });

  it('should create', () => {
    expect(TestBed.inject(PresupuestoService)).toBeTruthy();
  });
});
