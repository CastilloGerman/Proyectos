-- V3: Añadir columna recordatorio_enviado a facturas
-- Evita enviar recordatorios duplicados al mismo usuario.
ALTER TABLE public.facturas
    ADD COLUMN IF NOT EXISTS recordatorio_enviado BOOLEAN NOT NULL DEFAULT FALSE;
