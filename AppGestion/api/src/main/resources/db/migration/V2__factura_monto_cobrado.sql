-- V2: Añadir columna monto_cobrado a facturas
-- Permite registrar el importe real cobrado en facturas parciales.
ALTER TABLE public.facturas
    ADD COLUMN IF NOT EXISTS monto_cobrado DOUBLE PRECISION;
