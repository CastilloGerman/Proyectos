-- Valores por defecto en nuevas facturas (cobro a clientes)
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS default_metodo_pago VARCHAR(50);
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS default_condiciones_pago VARCHAR(200);
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS iban_cuenta VARCHAR(34);
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS bizum_telefono VARCHAR(20);
