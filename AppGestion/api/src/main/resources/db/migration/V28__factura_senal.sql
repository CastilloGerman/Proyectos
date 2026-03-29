-- Señal del presupuesto copiada a la factura (instantánea en el PDF)
ALTER TABLE facturas ADD COLUMN IF NOT EXISTS senal_importe DOUBLE PRECISION;
ALTER TABLE facturas ADD COLUMN IF NOT EXISTS senal_pagada BOOLEAN DEFAULT FALSE;
