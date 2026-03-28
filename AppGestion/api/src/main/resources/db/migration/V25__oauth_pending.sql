-- Estado OAuth (CSRF + PKCE) antes del callback
CREATE TABLE oauth_pending (
    id BIGSERIAL PRIMARY KEY,
    state_token VARCHAR(64) NOT NULL UNIQUE,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    provider VARCHAR(32) NOT NULL,
    code_verifier VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_oauth_pending_expires ON oauth_pending(expires_at);
