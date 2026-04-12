import { describe, it, expect, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { SupportApiService } from './support-api.service';

describe('SupportApiService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
  });

  it('should create', () => {
    expect(TestBed.inject(SupportApiService)).toBeTruthy();
  });
});
