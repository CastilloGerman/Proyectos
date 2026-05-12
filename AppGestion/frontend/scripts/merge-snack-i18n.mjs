/**
 * Copies `snack` from es.json and merges locale-specific overrides from snack.*.partial.json.
 * Also sets common.configure and auth.{register.registerError,forgot,passwordReset} from partials.
 * Run from repo root: node frontend/scripts/merge-snack-i18n.mjs
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const i18nDir = path.join(__dirname, '..', 'src', 'assets', 'i18n');

const es = JSON.parse(fs.readFileSync(path.join(i18nDir, 'es.json'), 'utf8'));

for (const loc of ['en', 'fr', 'ro', 'uk']) {
  const fp = path.join(i18nDir, `${loc}.json`);
  const partialPath = path.join(i18nDir, `snack.${loc}.partial.json`);
  const j = JSON.parse(fs.readFileSync(fp, 'utf8'));
  const partial = JSON.parse(fs.readFileSync(partialPath, 'utf8'));

  j.snack = partial.snack;
  j.common = { ...j.common, configure: partial.common.configure };

  j.auth.register = {
    ...j.auth.register,
    registerError: partial.auth.register.registerError,
  };
  j.auth.forgot = partial.auth.forgot;
  j.auth.passwordReset = partial.auth.passwordReset;

  // Parity guard: snack keys match Spanish
  const esKeys = Object.keys(es.snack).sort();
  const locKeys = Object.keys(j.snack).sort();
  if (JSON.stringify(esKeys) !== JSON.stringify(locKeys)) {
    throw new Error(`snack keys mismatch for ${loc}: ${esKeys.length} vs ${locKeys.length}`);
  }

  fs.writeFileSync(fp, JSON.stringify(j, null, 2) + '\n', 'utf8');
  console.log('updated', loc);
}
