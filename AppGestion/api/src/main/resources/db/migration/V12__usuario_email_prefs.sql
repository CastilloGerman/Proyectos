-- Preferencias de correo a nivel cuenta (configuración de notificaciones).
ALTER TABLE usuarios ADD COLUMN email_notify_billing BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE usuarios ADD COLUMN email_notify_documents BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE usuarios ADD COLUMN email_notify_marketing BOOLEAN NOT NULL DEFAULT false;
