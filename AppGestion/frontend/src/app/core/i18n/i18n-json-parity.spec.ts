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

  it('provides localized AI invalid-response and previous-line price explanations', () => {
    const prefixes: Record<(typeof locales)[number], string> = {
      es: 'La IA',
      en: 'The AI',
      fr: 'L’IA',
      ro: 'IA',
      uk: 'ШІ',
    };
    for (const locale of locales) {
      const raw = fs.readFileSync(path.join(dir, `${locale}.json`), 'utf-8');
      const ai = JSON.parse(raw).budQuick.ai;
      expect(ai.invalidResponse.startsWith(prefixes[locale]), locale);
      expect(ai.invalidResponse.length).toBeGreaterThan(50);
      expect(ai.priceIncludedInPreviousLine.length).toBeGreaterThan(10);
    }
  });

  it('provides translated AI entry, overwrite warnings and price guidance in every locale', () => {
    for (const locale of locales) {
      const raw = fs.readFileSync(path.join(dir, `${locale}.json`), 'utf-8');
      const translations = JSON.parse(raw);
      expect(translations.budList.createWithAi.length).toBeGreaterThan(5);
      expect(translations.budQuick.ai.sentWarning.length).toBeGreaterThan(20);
      expect(translations.budQuick.ai.nonPendingWarning.length).toBeGreaterThan(20);
      expect(translations.budQuick.aiHelp.step3.length).toBeGreaterThan(40);
      expect(translations.budQuick.aiHelp.note.length).toBeGreaterThan(10);
      expect(translations.budQuick.ai.priceTip.length).toBeGreaterThan(20);
    }
  });
});
