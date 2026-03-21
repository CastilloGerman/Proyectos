-- Avisos in-app por usuario (generales y personalizados: suscripción, facturas, etc.)
CREATE TABLE notificaciones (
    id              BIGSERIAL PRIMARY KEY,
    usuario_id      BIGINT NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    tipo            VARCHAR(40) NOT NULL,
    severidad       VARCHAR(20) NOT NULL DEFAULT 'INFO',
    titulo          VARCHAR(200) NOT NULL,
    resumen         VARCHAR(1000),
    leida           BOOLEAN NOT NULL DEFAULT FALSE,
    action_path     VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notificaciones_usuario_created ON notificaciones (usuario_id, created_at DESC);
CREATE INDEX idx_notificaciones_usuario_no_leida ON notificaciones (usuario_id) WHERE leida = FALSE;
