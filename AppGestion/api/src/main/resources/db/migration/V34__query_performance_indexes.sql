-- Parte 4 — Índices de rendimiento (sin solapar V21/V18/V19/V24).
-- V21 ya define idx_audit_access_org_occurred (organization_id, occurred_at DESC).
-- V24 ya define idx_email_jobs_status_next (status, next_retry_at, created_at).
-- V18 ya cubre notificaciones por usuario / no leídas.
--
-- Añade solo:
-- - Purga audit por occurred_at (DELETE ... WHERE occurred_at < :cutoff) sin filtrar organization_id.
-- - Purga sesiones por expires_at.
-- - Listado por usuario ordenado por last_activity_at sin orden extra en memoria.

CREATE INDEX IF NOT EXISTS idx_audit_access_occurred_at
    ON public.audit_access_event (occurred_at);

CREATE INDEX IF NOT EXISTS idx_usuario_sesion_expires_at
    ON public.usuario_sesion (expires_at);

CREATE INDEX IF NOT EXISTS idx_usuario_sesion_usuario_last_activity
    ON public.usuario_sesion (usuario_id, last_activity_at DESC);
