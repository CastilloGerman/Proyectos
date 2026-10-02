ALTER TABLE public.presupuestos
    ADD COLUMN enviado_at TIMESTAMP NULL,
    ADD COLUMN canal_envio VARCHAR(20) NULL;

ALTER TABLE public.presupuestos
    ADD CONSTRAINT ck_presupuestos_canal_envio
    CHECK (canal_envio IS NULL OR canal_envio IN ('WHATSAPP', 'EMAIL'));
