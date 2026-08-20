-- Foto in-situ y firma del cliente, 1:1 con el presupuesto (nullable).
-- BYTEA como en empresas.logo_imagen / firma_imagen; sin object storage.
ALTER TABLE public.presupuestos ADD COLUMN IF NOT EXISTS foto_trabajo BYTEA;
ALTER TABLE public.presupuestos ADD COLUMN IF NOT EXISTS firma_cliente BYTEA;
