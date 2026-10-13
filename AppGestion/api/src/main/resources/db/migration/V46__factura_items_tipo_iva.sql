-- Tipo de IVA por línea de factura: 21, 10, 4 o 0.
-- INTEGER (no SMALLINT): Hibernate mapea Integer a INTEGER y ddl-auto=validate fallaría con SMALLINT.
--
-- Antes de aplicar esto en una base con datos, ejecuta
-- src/main/resources/db/verification/V46_tipo_iva_precheck.sql
-- y revisa que no devuelva filas.
--
-- Este script solo rellena tipo_iva. No modifica facturas.subtotal, facturas.iva,
-- facturas.total ni factura_items.subtotal / cuota_iva.

ALTER TABLE factura_items
    ADD COLUMN tipo_iva INTEGER;

UPDATE factura_items AS i
SET tipo_iva = CASE
    WHEN COALESCE(f.iva_habilitado, TRUE) AND COALESCE(i.aplica_iva, TRUE) THEN 21
    ELSE 0
END
FROM facturas AS f
WHERE f.id = i.factura_id;

UPDATE factura_items
SET tipo_iva = 0
WHERE tipo_iva IS NULL;

ALTER TABLE factura_items
    ALTER COLUMN tipo_iva SET NOT NULL;

ALTER TABLE factura_items
    ADD CONSTRAINT ck_factura_items_tipo_iva CHECK (tipo_iva IN (21, 10, 4, 0));
