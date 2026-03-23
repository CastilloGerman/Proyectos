-- Registro inmutable de eventos de acceso y seguridad (auditoría por organización).
CREATE TABLE public.audit_access_event (
    id                 BIGSERIAL PRIMARY KEY,
    organization_id    BIGINT       NOT NULL REFERENCES public.organizations (id) ON DELETE CASCADE,
    usuario_id         BIGINT       REFERENCES public.usuarios (id) ON DELETE SET NULL,
    actor_email        VARCHAR(150),
    occurred_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    event_type         VARCHAR(64)  NOT NULL,
    success            BOOLEAN      NOT NULL,
    failure_reason     VARCHAR(255),
    ip_address         VARCHAR(45),
    ip_anonymized      BOOLEAN      NOT NULL DEFAULT FALSE,
    user_agent         TEXT,
    country_code       VARCHAR(2),
    session_id         VARCHAR(40),
    resource_path      VARCHAR(512),
    trace_id           VARCHAR(64),
    sensitive          BOOLEAN      NOT NULL DEFAULT FALSE,
    metadata_json      TEXT
);

CREATE INDEX idx_audit_access_org_occurred ON public.audit_access_event (organization_id, occurred_at DESC);
CREATE INDEX idx_audit_access_usuario_occurred ON public.audit_access_event (usuario_id, occurred_at DESC);
CREATE INDEX idx_audit_access_event_type ON public.audit_access_event (event_type);
CREATE INDEX idx_audit_access_success ON public.audit_access_event (success);

COMMENT ON TABLE public.audit_access_event IS 'Solo inserciones desde aplicación; retención vía job programado.';
