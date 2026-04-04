-- Numeración correlativa en fila + anulación lógica (sin DELETE físico)

ALTER TABLE facturas
    ADD COLUMN IF NOT EXISTS anio_factura INTEGER,
    ADD COLUMN IF NOT EXISTS numero_secuencial INTEGER,
    ADD COLUMN IF NOT EXISTS anulada BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS fecha_anulacion DATE,
    ADD COLUMN IF NOT EXISTS motivo_anulacion VARCHAR(255);

-- Año desde fecha de expedición
UPDATE facturas SET anio_factura = EXTRACT(YEAR FROM fecha_expedicion)::int WHERE anio_factura IS NULL;

-- Secuencial desde patrón FAC-AAAA-NNNN
UPDATE facturas
SET numero_secuencial = (regexp_match(numero_factura, '^FAC-[0-9]{4}-([0-9]+)$'))[1]::int
WHERE numero_factura ~ '^FAC-[0-9]{4}-[0-9]+$'
  AND numero_secuencial IS NULL;

-- Resto: numeración única por usuario y año (orden por id)
WITH ranked AS (
    SELECT id,
           usuario_id,
           anio_factura,
           ROW_NUMBER() OVER (PARTITION BY usuario_id, anio_factura ORDER BY id) AS rn
    FROM facturas
    WHERE numero_secuencial IS NULL
),
mx AS (
    SELECT usuario_id, anio_factura, COALESCE(MAX(numero_secuencial), 0) AS m
    FROM facturas
    WHERE numero_secuencial IS NOT NULL
    GROUP BY usuario_id, anio_factura
)
UPDATE facturas f
SET numero_secuencial = COALESCE(mx.m, 0) + r.rn
FROM ranked r
LEFT JOIN mx ON mx.usuario_id = r.usuario_id AND mx.anio_factura = r.anio_factura
WHERE f.id = r.id;

ALTER TABLE facturas ALTER COLUMN anio_factura SET NOT NULL;
ALTER TABLE facturas ALTER COLUMN numero_secuencial SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_facturas_usuario_anio_secuencial
    ON facturas (usuario_id, anio_factura, numero_secuencial);

-- Sincronizar contador global por usuario con el último año y máximo secuencial emitido
UPDATE factura_secuencia fs
SET anio = sub.max_anio,
    ultimo_numero = sub.max_sec
FROM (
    SELECT f.usuario_id,
           f.anio_factura AS max_anio,
           MAX(f.numero_secuencial) AS max_sec
    FROM facturas f
    INNER JOIN (
        SELECT usuario_id, MAX(anio_factura) AS max_anio
        FROM facturas
        GROUP BY usuario_id
    ) u ON u.usuario_id = f.usuario_id AND u.max_anio = f.anio_factura
    GROUP BY f.usuario_id, f.anio_factura
) sub
WHERE fs.usuario_id = sub.usuario_id;
