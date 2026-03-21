-- TOTP (2FA): secreto activo, bandera y enrolamiento pendiente con caducidad.
ALTER TABLE usuarios ADD COLUMN totp_secret VARCHAR(64);
ALTER TABLE usuarios ADD COLUMN totp_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuarios ADD COLUMN totp_pending_secret VARCHAR(64);
ALTER TABLE usuarios ADD COLUMN totp_pending_expires_at TIMESTAMP;
