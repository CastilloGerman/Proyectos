-- Ledger de suscripciones Stripe (sincronizado vía webhooks) y facturas de suscripción para informes/auditoría.
-- Los precios mensual/anual siguen en un único producto Stripe "Noemi" con dos Prices.

CREATE TABLE stripe_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    stripe_subscription_id VARCHAR(100) NOT NULL,
    stripe_price_id VARCHAR(100),
    stripe_status VARCHAR(30) NOT NULL,
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    current_period_start TIMESTAMPTZ,
    current_period_end TIMESTAMPTZ,
    trial_end TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_stripe_subscriptions_stripe_id UNIQUE (stripe_subscription_id)
);

CREATE INDEX idx_stripe_subscriptions_usuario_id ON stripe_subscriptions(usuario_id);

-- Facturas de cobro recurrente (Stripe Billing), no confundir con facturas B2B de negocio.
CREATE TABLE stripe_invoice_ledger (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    stripe_subscription_id VARCHAR(100),
    stripe_invoice_id VARCHAR(100) NOT NULL,
    amount_paid BIGINT NOT NULL DEFAULT 0,
    currency VARCHAR(10) NOT NULL DEFAULT 'eur',
    status VARCHAR(30),
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_stripe_invoice_ledger_invoice_id UNIQUE (stripe_invoice_id)
);

CREATE INDEX idx_stripe_invoice_ledger_usuario_id ON stripe_invoice_ledger(usuario_id);

ALTER TABLE usuarios ADD COLUMN stripe_price_id VARCHAR(100);
ALTER TABLE usuarios ADD COLUMN subscription_cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuarios ADD COLUMN subscription_requires_payment_action BOOLEAN NOT NULL DEFAULT FALSE;
