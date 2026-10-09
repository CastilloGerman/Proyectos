CREATE TABLE presupuesto_enlace (
    id BIGSERIAL PRIMARY KEY,
    presupuesto_id BIGINT NOT NULL REFERENCES presupuestos (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    creado_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expira_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revocado BOOLEAN NOT NULL DEFAULT FALSE,
    primera_vista_at TIMESTAMP WITH TIME ZONE,
    ultima_vista_at TIMESTAMP WITH TIME ZONE,
    num_vistas BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_presupuesto_enlace_presupuesto ON presupuesto_enlace (presupuesto_id);

ALTER TABLE presupuestos
    ADD COLUMN enlace_visto_notificado BOOLEAN NOT NULL DEFAULT FALSE;
