import assert from 'node:assert/strict';
import { mkdir, writeFile } from 'node:fs/promises';
import { request } from 'node:http';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawn } from 'node:child_process';
import test from 'node:test';

const __dirname = dirname(fileURLToPath(import.meta.url));
const browserDir = join(__dirname, 'dist/appgestion-frontend/browser');

async function waitForServer(child) {
  await new Promise((resolve, reject) => {
    const timeout = setTimeout(() => reject(new Error('server did not start')), 5_000);

    child.stdout.on('data', (chunk) => {
      if (chunk.toString().includes('AppGestion frontend listening')) {
        clearTimeout(timeout);
        resolve();
      }
    });

    child.once('exit', (code, signal) => {
      clearTimeout(timeout);
      reject(new Error(`server exited before listening: code=${code} signal=${signal}`));
    });
  });
}

function get(pathname, port) {
  return new Promise((resolve, reject) => {
    const req = request({ hostname: '127.0.0.1', port, path: pathname, method: 'GET' }, (res) => {
      res.resume();
      res.on('end', () => resolve(res.statusCode));
    });
    req.on('error', reject);
    req.end();
  });
}

test('malformed URL escape returns 400 without crashing the static server', async () => {
  await mkdir(browserDir, { recursive: true });
  await writeFile(join(browserDir, 'index.html'), '<!doctype html><title>AppGestion</title>');

  const port = 46_217;
  const child = spawn(process.execPath, ['server.mjs'], {
    cwd: __dirname,
    env: { ...process.env, PORT: String(port), FALLBACK_PORTS: '' },
    stdio: ['ignore', 'pipe', 'pipe'],
  });

  try {
    await waitForServer(child);

    assert.equal(await get('/%', port), 400);
    assert.equal(child.exitCode, null);
    assert.equal(await get('/', port), 200);
  } finally {
    child.kill();
  }
});
