-- Anticipo fiscal: columnas nuevas en presupuestos
ALTER TABLE presupuestos ADD COLUMN IF NOT EXISTS tiene_anticipo BOOLEAN DEFAULT FALSE;
ALTER TABLE presupuestos ADD COLUMN IF NOT EXISTS importe_anticipo NUMERIC(10,2);
ALTER TABLE presupuestos ADD COLUMN IF NOT EXISTS anticipo_facturado BOOLEAN DEFAULT FALSE;
ALTER TABLE presupuestos ADD COLUMN IF NOT EXISTS fecha_anticipo DATE;

-- Migrar datos desde señal (modelo antiguo simplificado)
UPDATE presupuestos
SET tiene_anticipo = TRUE,
    importe_anticipo = ROUND(senal_importe::numeric, 2),
    fecha_anticipo = CAST(fecha_creacion AS date),
    anticipo_facturado = FALSE
WHERE senal_importe IS NOT NULL AND senal_importe > 0;

ALTER TABLE presupuestos DROP COLUMN IF EXISTS senal_importe;
ALTER TABLE presupuestos DROP COLUMN IF EXISTS senal_pagada;

-- Facturas: tipo y vínculo anticipo
ALTER TABLE facturas ADD COLUMN IF NOT EXISTS tipo_factura VARCHAR(20) DEFAULT 'NORMAL';
ALTER TABLE facturas ADD COLUMN IF NOT EXISTS factura_anticipo_id BIGINT;
ALTER TABLE facturas ADD COLUMN IF NOT EXISTS importe_anticipo_descontado NUMERIC(10,2);

UPDATE facturas SET tipo_factura = 'NORMAL' WHERE tipo_factura IS NULL;

ALTER TABLE facturas DROP COLUMN IF EXISTS senal_importe;
ALTER TABLE facturas DROP COLUMN IF EXISTS senal_pagada;

ALTER TABLE facturas
    ADD CONSTRAINT fk_factura_anticipo FOREIGN KEY (factura_anticipo_id) REFERENCES facturas(id) ON DELETE RESTRICT;

CREATE UNIQUE INDEX IF NOT EXISTS uq_factura_anticipo_por_presu
    ON facturas (presupuesto_id) WHERE tipo_factura = 'ANTICIPO' AND presupuesto_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_factura_final_anticipo_por_presu
    ON facturas (presupuesto_id) WHERE tipo_factura = 'FINAL_CON_ANTICIPO' AND presupuesto_id IS NOT NULL;
