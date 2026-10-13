-- Facturas normales salidas de un presupuesto que tiene descuento
-- (global o de alguna línea). No modifica datos.
-- La diferencia es factura menos presupuesto: positivo significa que la factura
-- quedó por encima. No incluye anticipos ni facturas finales, porque su total
-- es solo el tramo que les toca.
-- PostgreSQL. No forma parte de Flyway.

WITH presupuestos_con_descuento AS (
    SELECT p.id,
           p.subtotal,
           p.iva,
           p.total,
           ROUND(CAST(COALESCE(p.descuento_global_porcentaje, 0) AS numeric), 2) AS descuento_global_porcentaje,
           ROUND(CAST(COALESCE(p.descuento_global_fijo, 0) AS numeric), 2) AS descuento_global_fijo,
           COALESCE(SUM(CASE
               WHEN COALESCE(i.descuento_porcentaje, 0) <> 0 OR COALESCE(i.descuento_fijo, 0) <> 0 THEN 1
               ELSE 0
           END), 0) AS lineas_con_descuento
    FROM presupuestos p
    LEFT JOIN presupuesto_items i ON i.presupuesto_id = p.id
    GROUP BY p.id, p.subtotal, p.iva, p.total, p.descuento_global_porcentaje, p.descuento_global_fijo
    HAVING COALESCE(p.descuento_global_porcentaje, 0) <> 0
        OR COALESCE(p.descuento_global_fijo, 0) <> 0
        OR COUNT(*) FILTER (
            WHERE COALESCE(i.descuento_porcentaje, 0) <> 0 OR COALESCE(i.descuento_fijo, 0) <> 0
        ) > 0
)
SELECT COUNT(*) AS facturas,
       ROUND(COALESCE(SUM(CAST(f.total AS numeric) - CAST(p.total AS numeric)), 0), 2) AS diferencia_total,
       ROUND(COALESCE(SUM(CAST(f.subtotal AS numeric) - CAST(p.subtotal AS numeric)), 0), 2) AS diferencia_base,
       ROUND(COALESCE(SUM(CAST(f.iva AS numeric) - CAST(p.iva AS numeric)), 0), 2) AS diferencia_iva
FROM facturas f
JOIN presupuestos_con_descuento p ON p.id = f.presupuesto_id
WHERE COALESCE(f.tipo_factura, 'NORMAL') = 'NORMAL';

-- Detalle, por si el total anterior no es cero.
SELECT f.id AS factura_id,
       f.numero_factura,
       p.id AS presupuesto_id,
       p.descuento_global_porcentaje,
       p.descuento_global_fijo,
       p.lineas_con_descuento,
       ROUND(CAST(p.total AS numeric), 2) AS total_presupuesto,
       ROUND(CAST(f.total AS numeric), 2) AS total_factura,
       ROUND(CAST(f.total AS numeric) - CAST(p.total AS numeric), 2) AS diferencia_total
FROM facturas f
JOIN (
    SELECT p.id,
           p.total,
           ROUND(CAST(COALESCE(p.descuento_global_porcentaje, 0) AS numeric), 2) AS descuento_global_porcentaje,
           ROUND(CAST(COALESCE(p.descuento_global_fijo, 0) AS numeric), 2) AS descuento_global_fijo,
           COALESCE(SUM(CASE
               WHEN COALESCE(i.descuento_porcentaje, 0) <> 0 OR COALESCE(i.descuento_fijo, 0) <> 0 THEN 1
               ELSE 0
           END), 0) AS lineas_con_descuento
    FROM presupuestos p
    LEFT JOIN presupuesto_items i ON i.presupuesto_id = p.id
    GROUP BY p.id, p.total, p.descuento_global_porcentaje, p.descuento_global_fijo
    HAVING COALESCE(p.descuento_global_porcentaje, 0) <> 0
        OR COALESCE(p.descuento_global_fijo, 0) <> 0
        OR COUNT(*) FILTER (
            WHERE COALESCE(i.descuento_porcentaje, 0) <> 0 OR COALESCE(i.descuento_fijo, 0) <> 0
        ) > 0
) p ON p.id = f.presupuesto_id
WHERE COALESCE(f.tipo_factura, 'NORMAL') = 'NORMAL'
ORDER BY f.id;
