import { describe, expect, it } from 'vitest';
import { routes } from './app.routes';

describe('public budget route', () => {
  it('is registered outside the authenticated route group', () => {
    const route = routes.find(candidate => candidate.path === 'p/:token');
    expect(route).toBeDefined();
    expect(route?.canActivate).toBeUndefined();
    expect(route?.loadComponent).toBeTypeOf('function');
  });
});
