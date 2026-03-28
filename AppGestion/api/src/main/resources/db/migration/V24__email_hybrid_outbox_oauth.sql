-- Cola de correo (outbox); el worker procesa en segundo plano.
CREATE TABLE email_jobs (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    payload_json TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    next_retry_at TIMESTAMPTZ,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ,
    CONSTRAINT email_jobs_idempotency UNIQUE (idempotency_key)
);

CREATE INDEX idx_email_jobs_status_next ON email_jobs(status, next_retry_at, created_at);

COMMENT ON TABLE email_jobs IS 'Outbox de correos; API solo inserta PENDING, worker envía asíncronamente.';

-- Proveedor de envío por organización (empresa)
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS email_provider VARCHAR(32) NOT NULL DEFAULT 'system';
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS oauth_provider VARCHAR(32);
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS oauth_access_token_enc TEXT;
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS oauth_refresh_token_enc TEXT;
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS oauth_token_expires_at TIMESTAMPTZ;
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS oauth_connected_at TIMESTAMPTZ;
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS oauth_on_failure VARCHAR(16) NOT NULL DEFAULT 'system';
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS system_from_override VARCHAR(255);

COMMENT ON COLUMN empresas.email_provider IS 'system | gmail | outlook | smtp_legacy';
COMMENT ON COLUMN empresas.oauth_on_failure IS 'system = fallback a Resend/SaaS; fail = no enviar si OAuth falla';

-- Eventos de proveedor (webhooks deliverability)
CREATE TABLE email_webhook_events (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(32) NOT NULL,
    event_type VARCHAR(64),
    external_id VARCHAR(255),
    payload_json TEXT,
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_email_webhook_events_received ON email_webhook_events(received_at);
