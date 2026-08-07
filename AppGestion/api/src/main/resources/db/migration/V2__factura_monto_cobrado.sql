-- V2: Columna monto_cobrado en facturas (histórico: bases antiguas sin la columna).
-- Si flyway_schema_history marca V1 pero la tabla no se creó (p. ej. baseline manual
-- o estado inconsistente), recreamos facturas con la misma definición que V1 antes del ALTER.
CREATE TABLE IF NOT EXISTS public.facturas (
    id               BIGSERIAL PRIMARY KEY,
    usuario_id       BIGINT           NOT NULL REFERENCES public.usuarios(id),
    numero_factura   VARCHAR(50)      NOT NULL,
    cliente_id       BIGINT           NOT NULL REFERENCES public.clientes(id),
    presupuesto_id   BIGINT           REFERENCES public.presupuestos(id),
    fecha_creacion   TIMESTAMP        DEFAULT NOW(),
    fecha_expedicion DATE,
    fecha_operacion  DATE,
    fecha_vencimiento DATE,
    regimen_fiscal   VARCHAR(100)     DEFAULT 'Régimen general del IVA',
    condiciones_pago VARCHAR(100),
    moneda           VARCHAR(10)      DEFAULT 'EUR',
    subtotal         DOUBLE PRECISION NOT NULL DEFAULT 0,
    iva              DOUBLE PRECISION NOT NULL DEFAULT 0,
    total            DOUBLE PRECISION NOT NULL DEFAULT 0,
    iva_habilitado   BOOLEAN          NOT NULL DEFAULT TRUE,
    metodo_pago      VARCHAR(50)      DEFAULT 'Transferencia',
    estado_pago      VARCHAR(50)      DEFAULT 'No Pagada',
    monto_cobrado    DOUBLE PRECISION,
    notas            VARCHAR(1000),
    CONSTRAINT uq_numero_factura_usuario UNIQUE (numero_factura, usuario_id)
);

ALTER TABLE public.facturas
    ADD COLUMN IF NOT EXISTS monto_cobrado DOUBLE PRECISION;
