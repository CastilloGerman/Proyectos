-- Señal en presupuesto
ALTER TABLE presupuestos ADD COLUMN IF NOT EXISTS senal_importe DOUBLE PRECISION;
ALTER TABLE presupuestos ADD COLUMN IF NOT EXISTS senal_pagada BOOLEAN DEFAULT FALSE;

-- Enlace de pago Stripe (factura)
ALTER TABLE facturas ADD COLUMN IF NOT EXISTS payment_link_url TEXT;
ALTER TABLE facturas ADD COLUMN IF NOT EXISTS payment_link_id VARCHAR(120);

-- Historial de cobros parciales
CREATE TABLE IF NOT EXISTS factura_cobros (
    id BIGSERIAL PRIMARY KEY,
    factura_id BIGINT NOT NULL REFERENCES facturas(id) ON DELETE CASCADE,
    importe DOUBLE PRECISION NOT NULL,
    fecha DATE NOT NULL DEFAULT CURRENT_DATE,
    metodo VARCHAR(80),
    notas VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_factura_cobros_factura ON factura_cobros(factura_id);
