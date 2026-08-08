import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';

describe('authGuard', () => {
  let navigate: ReturnType<typeof vi.fn>;
  let isAuthenticated: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    navigate = vi.fn();
    isAuthenticated = vi.fn();
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: { isAuthenticated } },
        { provide: Router, useValue: { navigate } },
      ],
    });
  });

  it('allows access when user is authenticated', () => {
    isAuthenticated.mockReturnValue(true);
    const result = TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));
    expect(result).toBe(true);
    expect(navigate).not.toHaveBeenCalled();
  });

  it('redirects to login when user is not authenticated', () => {
    isAuthenticated.mockReturnValue(false);
    const result = TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));
    expect(result).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/login']);
  });
});
