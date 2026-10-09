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

  it('all locale JSON files are valid UTF-8 and contain no replacement or mojibake characters', () => {
    for (const locale of locales) {
      const bytes = fs.readFileSync(path.join(dir, `${locale}.json`));
      const raw = new TextDecoder('utf-8', { fatal: true }).decode(bytes);
      expect(raw, locale).not.toContain('\uFFFD');
      expect(raw, locale).not.toMatch(/\?{2,}|[\p{L}]\?+[\p{L}]/u);
      expect(() => JSON.parse(raw), locale).not.toThrow();
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

  it('uses the approved budget follow-up help text in every locale', () => {
    const expected = {
      es: [
        '¿Cómo funciona el seguimiento?',
        'Te avisamos cuando un presupuesto enviado lleva días sin respuesta.',
        'Si el cliente lo abrió y no contestó, es buen momento para llamarle.',
        'Cuando lo aceptes o rechaces, dejamos de avisarte.',
      ],
      en: [
        'How does follow-up work?',
        'We notify you when a sent quote has gone days without a reply.',
        "If the client opened it but hasn't answered, it's a good time to call.",
        'Once you mark it accepted or rejected, we stop reminding you.',
      ],
      fr: [
        'Comment fonctionne le suivi ?',
        'Nous vous prévenons quand un devis envoyé reste sans réponse depuis plusieurs jours.',
        "Si le client l'a ouvert sans répondre, c'est le bon moment pour l'appeler.",
        'Dès que vous l’acceptez ou le refusez, nous cessons de vous relancer.',
      ],
      ro: [
        'Cum funcționează urmărirea?',
        'Te anunțăm când o ofertă trimisă rămâne fără răspuns de câteva zile.',
        'Dacă clientul a deschis-o, dar nu a răspuns, e un moment bun să îl suni.',
        'Când o marchezi acceptată sau respinsă, nu te mai anunțăm.',
      ],
      uk: [
        'Як працює нагадування?',
        'Ми повідомимо вас, коли надісланий кошторис кілька днів лишається без відповіді.',
        'Якщо клієнт відкрив його, але не відповів, це слушний момент зателефонувати.',
        'Коли ви позначите його прийнятим або відхиленим, ми припинимо нагадувати.',
      ],
    } as const;

    for (const locale of locales) {
      const raw = fs.readFileSync(path.join(dir, `${locale}.json`), 'utf-8');
      const help = JSON.parse(raw).budgetFollowSettings;
      expect([help.hintTitle, help.hint1, help.hint2, help.hint3], locale).toEqual(expected[locale]);
    }
  });
});
