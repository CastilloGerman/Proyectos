import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const i18nDir = path.join(__dirname, '..', 'src', 'assets', 'i18n');
const langs = ['es', 'en', 'fr', 'ro', 'uk'];

for (const lang of langs) {
  const fragmentPath = path.join(__dirname, `about-${lang}.json`);
  const about = JSON.parse(fs.readFileSync(fragmentPath, 'utf8'));
  const filePath = path.join(i18nDir, `${lang}.json`);
  const j = JSON.parse(fs.readFileSync(filePath, 'utf8'));
  if (!j.auth) j.auth = {};
  j.auth.about = about;
  fs.writeFileSync(filePath, JSON.stringify(j, null, 2) + '\n', 'utf8');
}

console.log('Merged auth.about from', langs.map((l) => `about-${l}.json`).join(', '));
