ALTER TABLE presupuestos
    ADD COLUMN seguimiento_silenciado BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE empresas
    ADD COLUMN seguimiento_activo BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN seguimiento_dias_espera INTEGER NOT NULL DEFAULT 3,
    ADD COLUMN seguimiento_max_avisos INTEGER NOT NULL DEFAULT 2,
    ADD COLUMN seguimiento_email_resumen BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT ck_empresas_seguimiento_rangos
        CHECK (seguimiento_dias_espera BETWEEN 1 AND 30 AND seguimiento_max_avisos BETWEEN 1 AND 5);

CREATE TABLE presupuesto_seguimiento_aviso (
    id BIGSERIAL PRIMARY KEY,
    presupuesto_id BIGINT NOT NULL REFERENCES presupuestos (id) ON DELETE CASCADE,
    tipo VARCHAR(32) NOT NULL,
    numero_aviso INTEGER NOT NULL,
    creado_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_seguimiento_aviso_presupuesto_numero UNIQUE (presupuesto_id, numero_aviso)
);

CREATE INDEX idx_seguimiento_aviso_presupuesto_creado
    ON presupuesto_seguimiento_aviso (presupuesto_id, creado_at DESC);
