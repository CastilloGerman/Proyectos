-- Sesiones de inicio de sesión (JWT con claim sid) para listado y revocación por dispositivo.
CREATE TABLE usuario_sesion (
    id VARCHAR(36) PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_activity_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    ip_address VARCHAR(45),
    user_agent VARCHAR(512),
    browser VARCHAR(80),
    os_name VARCHAR(80),
    device_type VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    client_label VARCHAR(200)
);

CREATE INDEX idx_usuario_sesion_usuario ON usuario_sesion (usuario_id);
CREATE INDEX idx_usuario_sesion_usuario_active ON usuario_sesion (usuario_id) WHERE revoked_at IS NULL;
