-- Datos opcionales de perfil (fecha de nacimiento, género, nacionalidad y país de residencia).
ALTER TABLE public.usuarios
    ADD COLUMN fecha_nacimiento DATE,
    ADD COLUMN genero VARCHAR(32),
    ADD COLUMN nacionalidad_iso VARCHAR(2),
    ADD COLUMN pais_residencia_iso VARCHAR(2);
