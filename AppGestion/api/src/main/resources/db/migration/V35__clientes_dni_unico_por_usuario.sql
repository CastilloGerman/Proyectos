-- Normaliza DNI/NIF, consolida duplicados por usuario y blinda unicidad en producción.

-- 1) Normalizar formato almacenado (sin espacios/guiones/puntos, mayúsculas).
UPDATE public.clientes
SET dni = UPPER(REPLACE(REPLACE(REPLACE(BTRIM(dni), ' ', ''), '-', ''), '.', ''))
WHERE dni IS NOT NULL;

-- 2) Vacíos -> NULL para no bloquear clientes provisionales.
UPDATE public.clientes
SET dni = NULL
WHERE dni IS NOT NULL
  AND BTRIM(dni) = '';

-- 3) Consolidar duplicados por (usuario_id, dni): se conserva el menor id.
WITH canonical AS (
    SELECT usuario_id, dni, MIN(id) AS keep_id
    FROM public.clientes
    WHERE dni IS NOT NULL
      AND BTRIM(dni) <> ''
    GROUP BY usuario_id, dni
    HAVING COUNT(*) > 1
),
dups AS (
    SELECT c.id AS dup_id, ca.keep_id
    FROM public.clientes c
    JOIN canonical ca
      ON ca.usuario_id = c.usuario_id
     AND ca.dni = c.dni
    WHERE c.id <> ca.keep_id
)
UPDATE public.presupuestos p
SET cliente_id = d.keep_id
FROM dups d
WHERE p.cliente_id = d.dup_id;

WITH canonical AS (
    SELECT usuario_id, dni, MIN(id) AS keep_id
    FROM public.clientes
    WHERE dni IS NOT NULL
      AND BTRIM(dni) <> ''
    GROUP BY usuario_id, dni
    HAVING COUNT(*) > 1
),
dups AS (
    SELECT c.id AS dup_id, ca.keep_id
    FROM public.clientes c
    JOIN canonical ca
      ON ca.usuario_id = c.usuario_id
     AND ca.dni = c.dni
    WHERE c.id <> ca.keep_id
)
UPDATE public.facturas f
SET cliente_id = d.keep_id
FROM dups d
WHERE f.cliente_id = d.dup_id;

WITH canonical AS (
    SELECT usuario_id, dni, MIN(id) AS keep_id
    FROM public.clientes
    WHERE dni IS NOT NULL
      AND BTRIM(dni) <> ''
    GROUP BY usuario_id, dni
    HAVING COUNT(*) > 1
),
dups AS (
    SELECT c.id AS dup_id
    FROM public.clientes c
    JOIN canonical ca
      ON ca.usuario_id = c.usuario_id
     AND ca.dni = c.dni
    WHERE c.id <> ca.keep_id
)
DELETE FROM public.clientes c
USING dups d
WHERE c.id = d.dup_id;

-- 4) Garantía final en BD (permite múltiples NULL).
CREATE UNIQUE INDEX IF NOT EXISTS uq_clientes_usuario_dni
    ON public.clientes (usuario_id, dni)
    WHERE dni IS NOT NULL AND BTRIM(dni) <> '';
