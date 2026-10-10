ALTER TABLE presupuestos
    ADD COLUMN respuesta_cliente VARCHAR(10),
    ADD COLUMN respuesta_cliente_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN respuesta_cliente_mensaje VARCHAR(500);

ALTER TABLE empresas
    ADD COLUMN permitir_respuesta_cliente BOOLEAN NOT NULL DEFAULT TRUE;
