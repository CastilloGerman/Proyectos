import { describe, it, expect, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { AuditAccessApiService } from './audit-access-api.service';

describe('AuditAccessApiService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
  });

  it('should create', () => {
    expect(TestBed.inject(AuditAccessApiService)).toBeTruthy();
  });
});
