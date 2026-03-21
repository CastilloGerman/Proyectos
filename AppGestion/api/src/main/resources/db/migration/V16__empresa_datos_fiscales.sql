-- Datos fiscales del emisor (facturación / cumplimiento)
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS regimen_iva_principal VARCHAR(120);
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS descripcion_actividad_fiscal VARCHAR(500);
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS nif_intracomunitario VARCHAR(20);
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS epigrafe_iae VARCHAR(30);
