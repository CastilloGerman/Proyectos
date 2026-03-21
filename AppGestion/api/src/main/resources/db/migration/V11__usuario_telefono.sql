-- Teléfono de contacto opcional en el perfil del usuario (no sustituye datos fiscales de empresa).
ALTER TABLE usuarios ADD COLUMN telefono VARCHAR(30) NULL;
