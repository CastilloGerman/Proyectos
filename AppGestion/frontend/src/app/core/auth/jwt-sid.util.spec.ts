import { describe, it, expect } from 'vitest';
import { isJwtExpired, readJwtExpSeconds, readJwtSessionId } from './jwt-sid.util';

function makeJwt(payload: Record<string, unknown>): string {
  const header = btoa(JSON.stringify({ alg: 'HS256' }))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
  const body = btoa(JSON.stringify(payload))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
  return `${header}.${body}.signature`;
}

describe('readJwtSessionId', () => {
  it('returns undefined for null, undefined or empty token', () => {
    expect(readJwtSessionId(null)).toBeUndefined();
    expect(readJwtSessionId(undefined)).toBeUndefined();
    expect(readJwtSessionId('')).toBeUndefined();
  });

  it('returns undefined for malformed token without dots', () => {
    expect(readJwtSessionId('not-a-jwt')).toBeUndefined();
  });

  it('returns trimmed sid from valid JWT payload', () => {
    const token = makeJwt({ sid: '  session-123  ', sub: 'user' });
    expect(readJwtSessionId(token)).toBe('session-123');
  });

  it('returns undefined when sid claim is missing', () => {
    const token = makeJwt({ sub: 'user' });
    expect(readJwtSessionId(token)).toBeUndefined();
  });

  it('returns undefined when sid is empty or whitespace', () => {
    expect(readJwtSessionId(makeJwt({ sid: '' }))).toBeUndefined();
    expect(readJwtSessionId(makeJwt({ sid: '   ' }))).toBeUndefined();
  });

  it('decodes base64url payload with - and _ characters', () => {
    const payload = { sid: 'sid-with-special-chars' };
    const body = btoa(JSON.stringify(payload))
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '');
    const token = `header.${body}.sig`;
    expect(readJwtSessionId(token)).toBe('sid-with-special-chars');
  });

  it('returns undefined for invalid base64 payload', () => {
    expect(readJwtSessionId('a.!!!invalid!!!.b')).toBeUndefined();
  });
});

describe('isJwtExpired', () => {
  it('is false when the token has no exp claim', () => {
    expect(isJwtExpired(makeJwt({ sid: 's' }))).toBe(false);
    expect(readJwtExpSeconds(makeJwt({ sid: 's' }))).toBeUndefined();
  });

  it('is false for malformed tokens', () => {
    expect(isJwtExpired(null)).toBe(false);
    expect(isJwtExpired('not-a-jwt')).toBe(false);
  });

  it('is true when exp is in the past', () => {
    const token = makeJwt({ exp: 1_700_000_000 });
    expect(isJwtExpired(token, 1_700_000_000_000)).toBe(true);
  });

  it('is false when exp is still in the future', () => {
    const token = makeJwt({ exp: 1_700_000_100 });
    expect(isJwtExpired(token, 1_700_000_000_000)).toBe(false);
  });
});
