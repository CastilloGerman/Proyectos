-- Token y expiración para recuperación de contraseña
ALTER TABLE public.usuarios
    ADD COLUMN IF NOT EXISTS password_reset_token VARCHAR(255),
    ADD COLUMN IF NOT EXISTS password_reset_expires_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_usuarios_password_reset_token
    ON public.usuarios (password_reset_token)
    WHERE password_reset_token IS NOT NULL;
