CREATE TABLE IF NOT EXISTS invitaciones (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(150) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    rol VARCHAR(50) NOT NULL DEFAULT 'OPERARIO',
    inviter_usuario_id BIGINT NOT NULL REFERENCES usuarios (id),
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_invitaciones_email ON invitaciones (email);
