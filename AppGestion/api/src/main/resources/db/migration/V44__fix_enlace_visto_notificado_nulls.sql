-- Add missing column from V41 if it doesn't exist, then fix null values
ALTER TABLE presupuestos
    ADD COLUMN IF NOT EXISTS enlace_visto_notificado BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE presupuestos
SET enlace_visto_notificado = FALSE
WHERE enlace_visto_notificado IS NULL;
