-- SEC-15: pista de auditoría de facturas con hash encadenado (cada registro incluye el hash del anterior de la misma
-- factura, de modo que borrar o modificar un cambio histórico rompe la cadena y se detecta).
-- Sin FK a facturas a propósito: la auditoría debe sobrevivir aunque la factura se elimine.
CREATE TABLE factura_auditoria (
    id BIGSERIAL PRIMARY KEY,
    factura_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    evento VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    datos_json TEXT NOT NULL,
    prev_hash VARCHAR(64) NOT NULL,
    hash VARCHAR(64) NOT NULL
);

CREATE INDEX idx_factura_auditoria_factura ON factura_auditoria (factura_id, id);
CREATE INDEX idx_factura_auditoria_usuario ON factura_auditoria (usuario_id);
