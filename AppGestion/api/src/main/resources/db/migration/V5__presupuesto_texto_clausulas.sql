-- Texto adicional del presupuesto con variables {{cliente.nombre}}, {{total}}, etc. (sustituidas al generar PDF)
ALTER TABLE presupuestos ADD COLUMN IF NOT EXISTS texto_clausulas TEXT;
