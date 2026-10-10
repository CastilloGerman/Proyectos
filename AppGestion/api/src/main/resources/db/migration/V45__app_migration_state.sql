-- Tabla para registrar migraciones de aplicación ya ejecutadas (evita re-ejecutar en cada arranque en frío).
CREATE TABLE IF NOT EXISTS app_migration_state (
    key   VARCHAR(64) PRIMARY KEY,
    value TEXT NOT NULL,
    executed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
