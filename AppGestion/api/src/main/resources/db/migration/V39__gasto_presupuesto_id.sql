ALTER TABLE public.gastos
    ADD COLUMN presupuesto_id BIGINT NULL;

ALTER TABLE public.gastos
    ADD CONSTRAINT fk_gastos_presupuesto
    FOREIGN KEY (presupuesto_id) REFERENCES public.presupuestos(id) ON DELETE SET NULL;

CREATE INDEX idx_gastos_presupuesto_id ON public.gastos (presupuesto_id)
    WHERE presupuesto_id IS NOT NULL;
