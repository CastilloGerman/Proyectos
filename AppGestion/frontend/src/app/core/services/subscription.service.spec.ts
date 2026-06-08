import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { SubscriptionService } from './subscription.service';
import { environment } from '../../../environments/environment';

describe('SubscriptionService', () => {
  let service: SubscriptionService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(SubscriptionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(service).toBeTruthy();
  });

  it('sends the selected yearly billing period to checkout', () => {
    service.createCheckoutSession('YEARLY').subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/subscription/checkout`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ billingPeriod: 'YEARLY' });
    req.flush({ checkoutUrl: 'https://checkout.stripe.test/session' });
  });

  it('uses an empty checkout body for monthly default', () => {
    service.createCheckoutSession('MONTHLY').subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/subscription/checkout`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush({ checkoutUrl: 'https://checkout.stripe.test/session' });
  });
});
