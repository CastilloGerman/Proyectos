-- RD 1619/2012: fecha de expedición obligatoria; filas legacy sin valor usan la fecha de creación.
UPDATE public.facturas
SET fecha_expedicion = COALESCE(fecha_expedicion, fecha_operacion, DATE(fecha_creacion))
WHERE fecha_expedicion IS NULL;

ALTER TABLE public.facturas
    ALTER COLUMN fecha_expedicion SET NOT NULL;
