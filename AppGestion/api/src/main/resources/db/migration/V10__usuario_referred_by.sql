-- Quién refirió al usuario (enlace por correo). Opcional.
ALTER TABLE public.usuarios
    ADD COLUMN IF NOT EXISTS referred_by_usuario_id BIGINT REFERENCES public.usuarios(id);

CREATE INDEX IF NOT EXISTS idx_usuarios_referred_by ON public.usuarios(referred_by_usuario_id);
