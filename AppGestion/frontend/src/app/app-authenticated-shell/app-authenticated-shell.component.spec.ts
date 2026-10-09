import * as fs from 'node:fs';
import * as path from 'node:path';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { AppAuthenticatedShellComponent } from './app-authenticated-shell.component';
import { NotificacionDto } from '../core/services/notificaciones.service';

describe('AppAuthenticatedShell notifications', () => {
  it('renders backend-localized plain text using interpolation, never HTML binding', () => {
    const html = fs.readFileSync(path.join(process.cwd(), 'src', 'app', 'app-authenticated-shell', 'app-authenticated-shell.component.html'), 'utf8');
    expect(html).toContain('{{ n.titulo }}');
    expect(html).toContain('{{ n.resumen }}');
    expect(html).not.toContain('[innerHTML]');
  });

  it('opens notification action paths and marks unread notifications as read', () => {
    const shell = Object.create(AppAuthenticatedShellComponent.prototype) as {
      notificaciones: { markRead: ReturnType<typeof vi.fn> };
      notifPreviewItems: { update: ReturnType<typeof vi.fn> };
      router: { navigateByUrl: ReturnType<typeof vi.fn> };
      abrirNotificacionPreview: (notification: NotificacionDto, trigger: { closeMenu: () => void }) => void;
    };
    shell.notificaciones = { markRead: vi.fn(() => of(void 0)) };
    shell.notifPreviewItems = { update: vi.fn() };
    shell.router = { navigateByUrl: vi.fn() };
    const trigger = { closeMenu: vi.fn() };
    shell.abrirNotificacionPreview({
      id: 9, tipo: 'SISTEMA', severidad: 'INFO', titulo: 'Title <img src=x>', resumen: '<b>Summary</b>',
      leida: false, actionPath: '/presupuestos/42', createdAt: '2026-01-01T00:00:00Z',
    }, trigger);

    expect(trigger.closeMenu).toHaveBeenCalledOnce();
    expect(shell.notificaciones.markRead).toHaveBeenCalledWith(9);
    expect(shell.router.navigateByUrl).toHaveBeenCalledWith('/presupuestos/42');
  });

  it('rejects external notification action paths', () => {
    const shell = Object.create(AppAuthenticatedShellComponent.prototype) as {
      notificaciones: { markRead: ReturnType<typeof vi.fn> };
      router: { navigateByUrl: ReturnType<typeof vi.fn> };
      abrirNotificacionPreview: (notification: NotificacionDto, trigger: { closeMenu: () => void }) => void;
    };
    shell.notificaciones = { markRead: vi.fn(() => of(void 0)) };
    shell.router = { navigateByUrl: vi.fn() };
    shell.abrirNotificacionPreview({
      id: 10, tipo: 'SISTEMA', severidad: 'INFO', titulo: 'Title', resumen: null,
      leida: true, actionPath: '//example.test', createdAt: '2026-01-01T00:00:00Z',
    }, { closeMenu: vi.fn() });

    expect(shell.router.navigateByUrl).not.toHaveBeenCalled();
  });
});
