-- SEC-06: secretos cifrados en reposo (AES-GCM, prefijo "enc:v1:") -> las columnas necesitan más longitud.
-- Los valores existentes en texto plano se cifran al arrancar (EncryptedSecretsMigrationRunner).
ALTER TABLE usuarios ALTER COLUMN totp_secret TYPE VARCHAR(255);
ALTER TABLE usuarios ALTER COLUMN totp_pending_secret TYPE VARCHAR(255);
ALTER TABLE empresas ALTER COLUMN mail_password TYPE VARCHAR(512);

-- SEC-06: el token de recuperación pasa a guardarse como hash SHA-256. Los tokens previos (texto plano, validez 1 h)
-- dejan de ser válidos: el usuario solo tiene que solicitar un nuevo enlace.
UPDATE usuarios
   SET password_reset_token = NULL,
       password_reset_expires_at = NULL
 WHERE password_reset_token IS NOT NULL;

-- SEC-05: verificación de email. Las cuentas existentes se consideran verificadas (se añade la columna con
-- DEFAULT TRUE y después se cambia el valor por defecto a FALSE para las nuevas altas).
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE usuarios ALTER COLUMN email_verified SET DEFAULT FALSE;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS email_verification_token_hash VARCHAR(64);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS email_verification_expires_at TIMESTAMP;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS email_verification_sent_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_usuarios_email_verification_token_hash
    ON usuarios (email_verification_token_hash)
    WHERE email_verification_token_hash IS NOT NULL;
