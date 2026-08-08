-- Gastos/compras del autónomo (IVA soportado para Modelo 303).

CREATE TABLE gastos (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    proveedor VARCHAR(200) NOT NULL,
    concepto VARCHAR(500) NOT NULL,
    fecha DATE NOT NULL,
    base_imponible DOUBLE PRECISION NOT NULL DEFAULT 0,
    tipo_iva DOUBLE PRECISION NOT NULL DEFAULT 21,
    cuota_iva DOUBLE PRECISION NOT NULL DEFAULT 0,
    categoria VARCHAR(20) NOT NULL DEFAULT 'OTROS',
    fecha_creacion TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_gastos_usuario_id ON public.gastos (usuario_id);
CREATE INDEX IF NOT EXISTS idx_gastos_usuario_fecha ON public.gastos (usuario_id, fecha DESC);
