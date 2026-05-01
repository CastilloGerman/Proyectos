import { createReadStream, existsSync, statSync } from 'node:fs';
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
  '.webp': 'image/webp',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
};

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

function handleRequest(req, res) {
  const filePath = resolveRequestPath(req.url ?? '/');
  const stream = createReadStream(filePath);

  res.setHeader('Content-Type', contentTypes[extname(filePath)] ?? 'application/octet-stream');
  if (filePath !== indexFile) {
    res.setHeader('Cache-Control', 'public, max-age=31536000, immutable');
  } else {
    res.setHeader('Cache-Control', 'no-cache');
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
