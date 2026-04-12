import { describe, it, expect, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NotificacionesService } from './notificaciones.service';

describe('NotificacionesService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
  });

  it('should create', () => {
    expect(TestBed.inject(NotificacionesService)).toBeTruthy();
  });
});
