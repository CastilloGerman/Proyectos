ALTER TABLE public.clientes
    ADD COLUMN IF NOT EXISTS estado_cliente VARCHAR(20) NOT NULL DEFAULT 'PROVISIONAL';

UPDATE public.clientes
SET estado_cliente = 'COMPLETO'
WHERE dni IS NOT NULL
  AND direccion IS NOT NULL
  AND codigo_postal IS NOT NULL;
