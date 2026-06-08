import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { SubscriptionService } from './subscription.service';
import { environment } from '../../../environments/environment';

describe('SubscriptionService', () => {
  let http: HttpTestingController;
  let service: SubscriptionService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    http = TestBed.inject(HttpTestingController);
    service = TestBed.inject(SubscriptionService);
  });

  afterEach(() => {
    http.verify();
  });

  it('should create', () => {
    expect(service).toBeTruthy();
  });

  it('sends the yearly billing period to checkout', () => {
    service.createCheckoutSession('YEARLY').subscribe();

    const req = http.expectOne(`${environment.apiUrl}/subscription/checkout`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ billingPeriod: 'YEARLY' });
    req.flush({ checkoutUrl: 'https://checkout.stripe.test/yearly' });
  });

  it('keeps monthly checkout backwards-compatible with an empty body', () => {
    service.createCheckoutSession('MONTHLY').subscribe();

    const req = http.expectOne(`${environment.apiUrl}/subscription/checkout`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush({ checkoutUrl: 'https://checkout.stripe.test/monthly' });
  });
});
