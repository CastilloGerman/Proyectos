-- Parte 2 — Auditoría de base de datos (solo lectura)
-- Ejecutar contra la BD de la aplicación, esquema public.
-- Ejemplo: psql "$DATABASE_URL" -f scripts/db-audit-part2.sql
-- Archivar salida: redirigir stdout a docs/db-audit-part2-results.txt
--
-- NULLABLE intencionales (no sugerir NOT NULL ni cambios): docs/DB-AUDIT-INTENTIONAL-NULLABLES.md
-- Sección 6 (Parte 4): nullables candidatos excluyendo explícitamente clientes.dni, direccion,
-- codigo_postal, telefono, email, pais, provincia y el resto de filas intencionales del doc.

\echo '=== 1. TABLAS Y COLUMNAS (public) ==='
SELECT table_name, column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = 'public'
ORDER BY table_name, ordinal_position;

\echo '=== 2. FOREIGN KEYS SIN ÍNDICE EN COLUMNA FK ==='
SELECT conrelid::regclass AS tabla,
       conname AS constraint,
       a.attname AS columna
FROM pg_constraint c
JOIN pg_attribute a ON a.attnum = ANY(c.conkey)
    AND a.attrelid = c.conrelid
WHERE c.contype = 'f'
AND NOT EXISTS (
    SELECT 1 FROM pg_index i
    WHERE i.indrelid = c.conrelid
    AND a.attnum = ANY(i.indkey)
)
ORDER BY 1, 3;

\echo '=== 3. TABLAS SIN PRIMARY KEY ==='
SELECT table_name
FROM information_schema.tables t
WHERE table_schema = 'public'
AND table_type = 'BASE TABLE'
AND NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints tc
    WHERE tc.table_schema = 'public'
    AND tc.table_name = t.table_name
    AND tc.constraint_type = 'PRIMARY KEY'
)
ORDER BY 1;

\echo '=== 4. COLUMNAS NULLABLE (auditoría + etiqueta política intencional) ==='
SELECT c.table_name,
       c.column_name,
       c.data_type,
       CASE
           WHEN (c.table_name, c.column_name) IN (
               SELECT * FROM (VALUES
                   ('audit_access_event'::text, 'usuario_id'::text),
                   ('clientes', 'fecha_creacion'),
                   ('clientes', 'dni'),
                   ('clientes', 'direccion'),
                   ('clientes', 'codigo_postal'),
                   ('clientes', 'telefono'),
                   ('clientes', 'email'),
                   ('clientes', 'pais'),
                   ('clientes', 'provincia'),
                   ('facturas', 'fecha_creacion'),
                   ('materiales', 'fecha_creacion'),
                   ('presupuestos', 'fecha_creacion'),
                   ('presupuestos', 'estado')
               ) AS intentional(tbl, col)
           ) THEN '[NULLABLE INTENCIONAL - no modificar]'
           WHEN c.column_name = 'base_imponible' THEN '[IGNORAR - no existe en esquema AppGestion; cálculo en servicio]'
           ELSE '[CANDIDATO A REVISIÓN — no listado como intencional en DB-AUDIT-INTENTIONAL-NULLABLES.md]'
       END AS auditoria_nota
FROM information_schema.columns c
WHERE c.table_schema = 'public'
  AND c.is_nullable = 'YES'
  AND (
      c.column_name IN ('usuario_id', 'fecha_creacion', 'estado', 'total', 'base_imponible')
      OR (
          c.table_name = 'clientes'
          AND c.column_name IN (
              'dni', 'direccion', 'codigo_postal', 'telefono', 'email', 'pais', 'provincia'
          )
      )
  )
ORDER BY 1, 2;

\echo '=== 5. DATOS HUÉRFANOS (conteos) ==='
SELECT 'facturas sin usuario' AS chequeo, COUNT(*) AS huerfanos
FROM facturas f
LEFT JOIN usuarios u ON f.usuario_id = u.id
WHERE u.id IS NULL
UNION ALL
SELECT 'presupuestos sin usuario', COUNT(*)
FROM presupuestos p
LEFT JOIN usuarios u ON p.usuario_id = u.id
WHERE u.id IS NULL
UNION ALL
SELECT 'presupuestos sin cliente', COUNT(*)
FROM presupuestos p
LEFT JOIN clientes c ON p.cliente_id = c.id
WHERE c.id IS NULL
UNION ALL
SELECT 'clientes sin usuario', COUNT(*)
FROM clientes c
LEFT JOIN usuarios u ON c.usuario_id = u.id
WHERE u.id IS NULL
UNION ALL
SELECT 'materiales sin usuario', COUNT(*)
FROM materiales m
LEFT JOIN usuarios u ON m.usuario_id = u.id
WHERE u.id IS NULL
UNION ALL
SELECT 'factura_items sin factura', COUNT(*)
FROM factura_items fi
LEFT JOIN facturas f ON fi.factura_id = f.id
WHERE f.id IS NULL
UNION ALL
SELECT 'factura_cobros sin factura', COUNT(*)
FROM factura_cobros fc
LEFT JOIN facturas f ON fc.factura_id = f.id
WHERE f.id IS NULL
UNION ALL
SELECT 'presupuesto_items sin presupuesto', COUNT(*)
FROM presupuesto_items pi
LEFT JOIN presupuestos p ON pi.presupuesto_id = p.id
WHERE p.id IS NULL
UNION ALL
SELECT 'organization_members sin org', COUNT(*)
FROM organization_members om
LEFT JOIN organizations o ON om.organization_id = o.id
WHERE o.id IS NULL
UNION ALL
SELECT 'organization_members sin usuario', COUNT(*)
FROM organization_members om
LEFT JOIN usuarios u ON om.user_id = u.id
WHERE u.id IS NULL
UNION ALL
SELECT 'email_jobs sin usuario', COUNT(*)
FROM email_jobs ej
LEFT JOIN usuarios u ON ej.usuario_id = u.id
WHERE u.id IS NULL
UNION ALL
SELECT 'oauth_pending sin usuario', COUNT(*)
FROM oauth_pending op
LEFT JOIN usuarios u ON op.usuario_id = u.id
WHERE u.id IS NULL
UNION ALL
SELECT 'empresas sin usuario', COUNT(*)
FROM empresas e
LEFT JOIN usuarios u ON e.usuario_id = u.id
WHERE u.id IS NULL
ORDER BY 1;

\echo '=== 6. COLUMNAS NULLABLE CANDIDATAS (excl. política intencional Parte 4 / DB-AUDIT-INTENTIONAL-NULLABLES) ==='
-- Excluye explícitamente los 7 campos de clientes (dni, direccion, codigo_postal, telefono, email, pais, provincia)
-- más el resto de nullables intencionales documentados. No sugiere NOT NULL automáticamente: revisión manual + conteos en BD.
SELECT c.table_name,
       c.column_name,
       c.data_type
FROM information_schema.columns c
WHERE c.table_schema = 'public'
  AND c.is_nullable = 'YES'
  AND c.table_name NOT LIKE 'flyway\_%' ESCAPE '\'
  AND (c.table_name, c.column_name) NOT IN (
      SELECT * FROM (VALUES
          ('audit_access_event'::text, 'usuario_id'::text),
          ('clientes', 'fecha_creacion'),
          ('clientes', 'dni'),
          ('clientes', 'direccion'),
          ('clientes', 'codigo_postal'),
          ('clientes', 'telefono'),
          ('clientes', 'email'),
          ('clientes', 'pais'),
          ('clientes', 'provincia'),
          ('facturas', 'fecha_creacion'),
          ('materiales', 'fecha_creacion'),
          ('presupuestos', 'fecha_creacion'),
          ('presupuestos', 'estado')
      ) AS intentional(tbl, col)
  )
ORDER BY c.table_name, c.column_name;
