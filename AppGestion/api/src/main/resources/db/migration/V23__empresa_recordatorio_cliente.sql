-- Recordatorios automáticos de cobro al cliente (email), días tras el vencimiento
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS recordatorio_cliente_activo BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS recordatorio_cliente_dias VARCHAR(32) NOT NULL DEFAULT '7,15,30';

-- Qué envíos ya se hicieron por factura (lista "7,15,30" separada por comas)
ALTER TABLE facturas ADD COLUMN IF NOT EXISTS recordatorio_cliente_marcas VARCHAR(32);
