-- Verificación ANTES de V46. PostgreSQL. No forma parte de Flyway.
-- Usa el backfill previsto (iva_habilitado y aplica_iva → 21; si no, 0)
-- porque la columna tipo_iva todavía no existe.
-- Si alguna consulta devuelve filas, no migres hasta revisarlas.

-- 1) Suma de cuota_iva de las líneas que quedarían en tipo 21, frente a facturas.iva.
--    Tolerancia: 0,01 € por línea de la factura (acumulable).
SELECT f.id,
       f.numero_factura,
       ROUND(CAST(f.iva AS numeric), 2) AS iva_cabecera,
       ROUND(COALESCE(SUM(
           CASE
               WHEN COALESCE(f.iva_habilitado, TRUE) AND COALESCE(i.aplica_iva, TRUE)
                   THEN CAST(COALESCE(i.cuota_iva, 0) AS numeric)
               ELSE 0
           END
       ), 0), 2) AS suma_cuota_tipo_21,
       COUNT(i.id) AS lineas,
       ROUND(0.01 * COUNT(i.id), 2) AS tolerancia
FROM facturas f
LEFT JOIN factura_items i ON i.factura_id = f.id
GROUP BY f.id, f.numero_factura, f.iva
HAVING ABS(
    ROUND(CAST(f.iva AS numeric), 2)
    - ROUND(COALESCE(SUM(
        CASE
            WHEN COALESCE(f.iva_habilitado, TRUE) AND COALESCE(i.aplica_iva, TRUE)
                THEN CAST(COALESCE(i.cuota_iva, 0) AS numeric)
            ELSE 0
        END
    ), 0), 2)
) > ROUND(0.01 * COUNT(i.id), 2)
ORDER BY f.id;

-- 2) IVA de cabecera distinto de 0 y ninguna línea quedaría en tipo 21
--    (todas en tipo 0, o factura sin líneas).
--    O al revés: IVA de cabecera 0 y alguna línea quedaría en tipo 21.
SELECT f.id,
       f.numero_factura,
       ROUND(CAST(f.iva AS numeric), 2) AS iva_cabecera,
       COUNT(i.id) AS lineas,
       COUNT(i.id) FILTER (
           WHERE COALESCE(f.iva_habilitado, TRUE) AND COALESCE(i.aplica_iva, TRUE)
       ) AS lineas_tipo_21
FROM facturas f
LEFT JOIN factura_items i ON i.factura_id = f.id
GROUP BY f.id, f.numero_factura, f.iva
HAVING (
    ROUND(CAST(f.iva AS numeric), 2) <> 0
    AND COUNT(i.id) FILTER (
        WHERE COALESCE(f.iva_habilitado, TRUE) AND COALESCE(i.aplica_iva, TRUE)
    ) = 0
) OR (
    ROUND(CAST(f.iva AS numeric), 2) = 0
    AND COUNT(i.id) FILTER (
        WHERE COALESCE(f.iva_habilitado, TRUE) AND COALESCE(i.aplica_iva, TRUE)
    ) > 0
)
ORDER BY f.id;
