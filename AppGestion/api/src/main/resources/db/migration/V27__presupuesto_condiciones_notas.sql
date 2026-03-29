-- Sustituye texto libre con variables por condiciones predefinidas (JSON) + nota adicional.
ALTER TABLE public.presupuestos DROP COLUMN IF EXISTS texto_clausulas;
ALTER TABLE public.presupuestos ADD COLUMN IF NOT EXISTS condiciones_activas TEXT;
ALTER TABLE public.presupuestos ADD COLUMN IF NOT EXISTS nota_adicional TEXT;

-- Valores por defecto al crear presupuestos (JSON array de claves válidas).
ALTER TABLE public.usuarios ADD COLUMN IF NOT EXISTS condiciones_presupuesto_predeterminadas TEXT;
