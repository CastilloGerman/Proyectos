import { createHash } from 'node:crypto';
import { createReadStream, existsSync, readFileSync, statSync } from 'node:fs';
import { createServer } from 'node:http';
import { extname, join, normalize, resolve, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = fileURLToPath(new URL('.', import.meta.url));
const publicDir = resolve(__dirname, 'dist/appgestion-frontend/browser');
const indexFile = join(publicDir, 'index.html');
const port = Number.parseInt(process.env.PORT ?? '8080', 10);
const host = '0.0.0.0';
const fallbackPorts = (process.env.FALLBACK_PORTS ?? '4200,3000')
  .split(',')
  .map((value) => Number.parseInt(value.trim(), 10))
  .filter(Number.isInteger);
const ports = [...new Set([port, ...fallbackPorts])];

const contentTypes = {
  '.css': 'text/css; charset=utf-8',
  '.html': 'text/html; charset=utf-8',
  '.ico': 'image/x-icon',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.js': 'text/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.svg': 'image/svg+xml',
  '.webmanifest': 'application/manifest+json',
  '.webp': 'image/webp',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
};

function isRevalidatedAsset(pathname, filePath) {
  if (filePath === indexFile) return true;
  if (pathname.startsWith('/assets/i18n/') && extname(filePath) === '.json') return true;
  const base = pathname.split('/').pop() ?? '';
  return base === 'manifest.webmanifest' || base === 'ngsw.json' || base === 'ngsw-worker.js';
}

function resolveRequestPath(url) {
  const pathname = decodeURIComponent(new URL(url, 'http://localhost').pathname);
  const requested = normalize(join(publicDir, pathname));

  if (!requested.startsWith(publicDir + sep) && requested !== publicDir) {
    return indexFile;
  }

  if (existsSync(requested) && statSync(requested).isFile()) {
    return requested;
  }

  return indexFile;
}

/**
 * SEC-07: cabeceras de seguridad y CSP. Los scripts inline de index.html (p. ej. el del tema) se autorizan por hash
 * calculado al arrancar, de modo que no hace falta 'unsafe-inline' en script-src.
 */
function inlineScriptHashes() {
  try {
    const html = readFileSync(indexFile, 'utf8').replace(/\r\n/g, '\n');
    const hashes = [];
    const re = /<script(?![^>]*\bsrc=)[^>]*>([\s\S]*?)<\/script>/g;
    let m;
    while ((m = re.exec(html)) !== null) {
      hashes.push(`'sha256-${createHash('sha256').update(m[1]).digest('base64')}'`);
    }
    return hashes;
  } catch {
    return [];
  }
}

const apiOrigin = process.env.API_ORIGIN ?? 'https://api.noemiweb.com';
const csp = [
  "default-src 'self'",
  // 'unsafe-hashes' + hash: autoriza SOLO el manejador onload="this.media='all'" que Angular inyecta para cargar
  // styles.css sin bloquear el render (inlineCritical). No habilita scripts inline arbitrarios.
  `script-src 'self' ${inlineScriptHashes().join(' ')} 'unsafe-hashes' 'sha256-MhtPZXr7+LpJUY5qtMutB+qWfQtMaPccfe7QXtCcEYc=' https://accounts.google.com/gsi/`,
  "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://accounts.google.com/gsi/style",
  "font-src 'self' data: https://fonts.gstatic.com",
  "img-src 'self' data: blob: https:",
  "media-src 'self' blob: data:",
  `connect-src 'self' ${apiOrigin} https://accounts.google.com/gsi/`,
  "frame-src 'self' blob: https://accounts.google.com/gsi/",
  "worker-src 'self'",
  "manifest-src 'self'",
  "object-src 'none'",
  "base-uri 'self'",
  "form-action 'self'",
  "frame-ancestors 'none'",
].join('; ');

function setSecurityHeaders(res) {
  res.setHeader('Content-Security-Policy', csp);
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');
  // El botón de Google abre un popup y devuelve la cuenta elegida a esta página.
  // Sin allow-popups el popup se queda en blanco o no entrega la cuenta, y hay que reintentar.
  res.setHeader('Cross-Origin-Opener-Policy', 'same-origin-allow-popups');
  res.setHeader('Permissions-Policy', 'camera=(), microphone=(), geolocation=(), payment=()');
  res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains');
}

function handleRequest(req, res) {
  const filePath = resolveRequestPath(req.url ?? '/');
  const stream = createReadStream(filePath);
  const pathname = decodeURIComponent(new URL(req.url ?? '/', 'http://localhost').pathname);

  setSecurityHeaders(res);

  res.setHeader('Content-Type', contentTypes[extname(filePath)] ?? 'application/octet-stream');
  if (pathname.startsWith('/assets/i18n/') && extname(filePath) === '.json') {
    res.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate');
  } else if (isRevalidatedAsset(pathname, filePath)) {
    res.setHeader('Cache-Control', 'no-cache');
  } else {
    res.setHeader('Cache-Control', 'public, max-age=31536000, immutable');
  }

  stream.on('error', () => {
    res.writeHead(500);
    res.end('Internal server error');
  });
  stream.pipe(res);
}

for (const listenPort of ports) {
  const server = createServer(handleRequest);
  server.on('error', (error) => {
    if (error.code === 'EADDRINUSE') {
      console.warn(`Port ${listenPort} is already in use; skipping fallback listener`);
      return;
    }
    throw error;
  });
  server.listen(listenPort, host, () => {
    console.log(`AppGestion frontend listening on http://${host}:${listenPort}`);
  });
}
