import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { roleMutateGuard } from './role-mutate.guard';
import { AuthService } from './auth.service';

describe('roleMutateGuard', () => {
  let navigate: ReturnType<typeof vi.fn>;
  let canMutate: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    navigate = vi.fn();
    canMutate = vi.fn();
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: { canMutate } },
        { provide: Router, useValue: { navigate } },
      ],
    });
  });

  it('allows access when user can mutate', () => {
    canMutate.mockReturnValue(true);
    const result = TestBed.runInInjectionContext(() => roleMutateGuard({} as never, {} as never));
    expect(result).toBe(true);
    expect(navigate).not.toHaveBeenCalled();
  });

  it('redirects to dashboard when user cannot mutate', () => {
    canMutate.mockReturnValue(false);
    const result = TestBed.runInInjectionContext(() => roleMutateGuard({} as never, {} as never));
    expect(result).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/dashboard']);
  });
});
