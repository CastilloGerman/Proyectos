-- Rollback de V46__factura_items_tipo_iva.sql
--
-- Solo es válido antes de crear facturas con líneas al 10 % o al 4 %.
-- Si existe alguna línea con tipo_iva distinto de 21 o 0, el script aborta
-- y no borra la columna. No toca subtotal, iva, total ni cuota_iva.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM factura_items
        WHERE tipo_iva IS DISTINCT FROM 21
          AND tipo_iva IS DISTINCT FROM 0
    ) THEN
        RAISE EXCEPTION 'Rollback abortado: hay lineas con tipo_iva distinto de 21 o 0. Este script solo es valido antes de crear facturas al 10 por ciento o al 4 por ciento.';
    END IF;
END $$;

ALTER TABLE factura_items DROP CONSTRAINT IF EXISTS ck_factura_items_tipo_iva;
ALTER TABLE factura_items DROP COLUMN IF EXISTS tipo_iva;
