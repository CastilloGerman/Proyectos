import { describe, it, expect } from 'vitest';
import * as fs from 'node:fs';
import * as path from 'node:path';

function flattenKeys(obj: unknown, prefix = ''): string[] {
  if (obj === null || typeof obj !== 'object' || Array.isArray(obj)) {
    return prefix ? [prefix] : [];
  }
  const out: string[] = [];
  for (const k of Object.keys(obj as Record<string, unknown>).sort()) {
    const v = (obj as Record<string, unknown>)[k];
    const next = prefix ? `${prefix}.${k}` : k;
    if (v !== null && typeof v === 'object' && !Array.isArray(v)) {
      out.push(...flattenKeys(v, next));
    } else {
      out.push(next);
    }
  }
  return out;
}

describe('i18n JSON parity', () => {
  const dir = path.join(process.cwd(), 'src', 'assets', 'i18n');
  const locales = ['es', 'en', 'fr', 'ro', 'uk'] as const;

  it('every locale file has the same key set as es.json', () => {
    const baseRaw = fs.readFileSync(path.join(dir, 'es.json'), 'utf-8');
    const baseKeys = flattenKeys(JSON.parse(baseRaw)).sort();
    expect(baseKeys.length).toBeGreaterThan(10);

    for (const loc of locales) {
      if (loc === 'es') continue;
      const raw = fs.readFileSync(path.join(dir, `${loc}.json`), 'utf-8');
      const keys = flattenKeys(JSON.parse(raw)).sort();
      expect(keys, `Keys mismatch for ${loc}`).toEqual(baseKeys);
    }
  });
});
