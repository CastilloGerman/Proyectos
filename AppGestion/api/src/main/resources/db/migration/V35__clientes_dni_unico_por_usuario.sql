-- Normaliza DNI/NIF y blinda unicidad por usuario sin borrar clientes existentes.

-- 1) Normalizar formato almacenado (sin espacios/guiones/puntos, mayúsculas).
UPDATE public.clientes
SET dni = UPPER(REPLACE(REPLACE(REPLACE(BTRIM(dni), ' ', ''), '-', ''), '.', ''))
WHERE dni IS NOT NULL;

-- 2) Vacíos -> NULL para no bloquear clientes provisionales.
UPDATE public.clientes
SET dni = NULL
WHERE dni IS NOT NULL
  AND BTRIM(dni) = '';

-- 3) No consolidar automáticamente duplicados: dos clientes con el mismo DNI pueden
-- tener datos/contactos/historial distintos. Borrar filas aquí perdería datos de usuario.
DO $$
DECLARE
    duplicate_groups BIGINT;
BEGIN
    SELECT COUNT(*)
    INTO duplicate_groups
    FROM (
        SELECT usuario_id, dni
        FROM public.clientes
        WHERE dni IS NOT NULL
          AND BTRIM(dni) <> ''
        GROUP BY usuario_id, dni
        HAVING COUNT(*) > 1
    ) duplicated;

    IF duplicate_groups > 0 THEN
        RAISE EXCEPTION
            'V35 abortada: existen % grupos de clientes con DNI/NIF duplicado por usuario. Resolverlos manualmente antes de crear uq_clientes_usuario_dni para evitar pérdida de datos.',
            duplicate_groups;
    END IF;
END $$;

-- 4) Garantía final en BD (permite múltiples NULL).
CREATE UNIQUE INDEX IF NOT EXISTS uq_clientes_usuario_dni
    ON public.clientes (usuario_id, dni)
    WHERE dni IS NOT NULL AND BTRIM(dni) <> '';
