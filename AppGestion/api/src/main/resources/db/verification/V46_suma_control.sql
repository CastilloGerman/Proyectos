-- Suma de control de importes. Ejecútala ANTES y DESPUÉS de V46 y compara el resultado.
-- V46 solo añade tipo_iva: estas cifras tienen que salir idénticas.
-- PostgreSQL. No forma parte de Flyway.

-- Detalle por factura.
SELECT f.id,
       f.numero_factura,
       ROUND(CAST(f.subtotal AS numeric), 2) AS subtotal,
       ROUND(CAST(f.iva AS numeric), 2) AS iva,
       ROUND(CAST(f.total AS numeric), 2) AS total
FROM facturas f
ORDER BY f.id;

-- Una sola fila para comparar las dos ejecuciones de un vistazo.
-- Los importes de cabecera y los de línea van en consultas aparte para no multiplicar
-- el total de la factura por el número de líneas.
SELECT
    (SELECT COUNT(*) FROM facturas) AS facturas,
    (SELECT ROUND(COALESCE(SUM(CAST(subtotal AS numeric)), 0), 2) FROM facturas) AS suma_subtotal,
    (SELECT ROUND(COALESCE(SUM(CAST(iva AS numeric)), 0), 2) FROM facturas) AS suma_iva,
    (SELECT ROUND(COALESCE(SUM(CAST(total AS numeric)), 0), 2) FROM facturas) AS suma_total,
    (SELECT COUNT(*) FROM factura_items) AS lineas,
    (SELECT ROUND(COALESCE(SUM(CAST(subtotal AS numeric)), 0), 2) FROM factura_items) AS suma_lineas_subtotal,
    (SELECT ROUND(COALESCE(SUM(CAST(COALESCE(cuota_iva, 0) AS numeric)), 0), 2) FROM factura_items) AS suma_lineas_cuota;
