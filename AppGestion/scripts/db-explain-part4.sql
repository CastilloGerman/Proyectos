-- Parte 4 — EXPLAIN de consultas calientes (PostgreSQL)
-- Ejecutar: psql ... -f scripts/db-explain-part4.sql
-- Salida recomendada: docs/db-explain-part4-results.txt
-- Usa ROLLBACK en deletes de prueba para no modificar datos.

\echo '=== audit_access_event: listado por organización y fecha (histórico accesos) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT a.id, a.occurred_at, a.event_type
FROM public.audit_access_event a
WHERE a.organization_id = (SELECT o.id FROM public.organizations o ORDER BY o.id LIMIT 1)
ORDER BY a.occurred_at DESC
LIMIT 50;

\echo '=== audit_access_event: borrado por antigüedad (job limpieza) — ANALYZE + ROLLBACK ==='
BEGIN;
EXPLAIN (ANALYZE, BUFFERS)
DELETE FROM public.audit_access_event a
WHERE a.occurred_at < TIMESTAMPTZ '1970-01-01';
ROLLBACK;

\echo '=== email_jobs: cola pendiente ordenada por created_at ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT j.id, j.status, j.created_at, j.next_retry_at
FROM public.email_jobs j
WHERE j.status = 'PENDING'
  AND (j.next_retry_at IS NULL OR j.next_retry_at <= now())
ORDER BY j.created_at ASC
LIMIT 50;

\echo '=== notificaciones: por usuario y filtro leída ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT n.id, n.leida, n.created_at
FROM public.notificaciones n
WHERE n.usuario_id = (SELECT u.id FROM public.usuarios u ORDER BY u.id LIMIT 1)
  AND n.leida = false
ORDER BY n.created_at DESC
LIMIT 50;

\echo '=== usuario_sesion: borrado por expiración — ANALYZE + ROLLBACK ==='
BEGIN;
EXPLAIN (ANALYZE, BUFFERS)
DELETE FROM public.usuario_sesion s
WHERE s.expires_at < TIMESTAMPTZ '1970-01-01';
ROLLBACK;

\echo '=== usuario_sesion: sesiones activas por usuario ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT s.id, s.last_activity_at
FROM public.usuario_sesion s
WHERE s.usuario_id = (SELECT u.id FROM public.usuarios u ORDER BY u.id LIMIT 1)
ORDER BY s.last_activity_at DESC;
