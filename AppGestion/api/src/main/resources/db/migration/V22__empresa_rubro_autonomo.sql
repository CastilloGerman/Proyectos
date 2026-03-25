-- Rubro/actividad (solo métricas internas; no se usa en PDFs de factura/presupuesto)
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS rubro_autonomo_codigo VARCHAR(64);
