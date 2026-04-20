-- V2: Añadir columna monto_cobrado a facturas
-- Permite registrar el importe real cobrado en facturas parciales.
-- (V1 ya incluye esta columna en el CREATE; este ALTER es idempotente para bases antiguas.)
ALTER TABLE public.facturas
    ADD COLUMN IF NOT EXISTS monto_cobrado DOUBLE PRECISION;
