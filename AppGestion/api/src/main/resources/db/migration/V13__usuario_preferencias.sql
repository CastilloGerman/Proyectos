-- Idioma de interfaz, zona horaria (IANA) y moneda por defecto para importes en UI/PDF (no sustituye datos fiscales de empresa).
ALTER TABLE usuarios ADD COLUMN ui_locale VARCHAR(10) NOT NULL DEFAULT 'es';
ALTER TABLE usuarios ADD COLUMN time_zone VARCHAR(64) NOT NULL DEFAULT 'Europe/Madrid';
ALTER TABLE usuarios ADD COLUMN currency_code VARCHAR(3) NOT NULL DEFAULT 'EUR';
