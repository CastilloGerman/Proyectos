-- V1: Esquema inicial de AppGestion
-- Generado a partir de las entidades JPA existentes.
-- En producción Flyway gestiona el esquema; ddl-auto=validate.

CREATE TABLE IF NOT EXISTS public.usuarios (
    id                              BIGSERIAL PRIMARY KEY,
    nombre                          VARCHAR(100)  NOT NULL,
    email                           VARCHAR(150)  NOT NULL UNIQUE,
    password_hash                   VARCHAR(255)  NOT NULL,
    rol                             VARCHAR(50)   DEFAULT 'USER',
    activo                          BOOLEAN       NOT NULL DEFAULT TRUE,
    fecha_creacion                  TIMESTAMP     NOT NULL DEFAULT NOW(),
    stripe_customer_id              VARCHAR(100),
    stripe_subscription_id          VARCHAR(100),
    subscription_status             VARCHAR(30),
    subscription_current_period_end TIMESTAMP,
    trial_start_date                DATE,
    trial_end_date                  DATE
);

CREATE TABLE IF NOT EXISTS public.empresas (
    id                       BIGSERIAL PRIMARY KEY,
    usuario_id               BIGINT NOT NULL UNIQUE REFERENCES public.usuarios(id),
    nombre                   VARCHAR(200) NOT NULL DEFAULT '',
    direccion                VARCHAR(255),
    codigo_postal            VARCHAR(10),
    provincia                VARCHAR(100),
    pais                     VARCHAR(100) DEFAULT 'España',
    nif                      VARCHAR(20),
    telefono                 VARCHAR(50),
    email                    VARCHAR(150),
    notas_pie_presupuesto    VARCHAR(1000),
    notas_pie_factura        VARCHAR(1000),
    mail_host                VARCHAR(100),
    mail_port                INTEGER,
    mail_username            VARCHAR(150),
    mail_password            VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS public.clientes (
    id             BIGSERIAL PRIMARY KEY,
    usuario_id     BIGINT       NOT NULL REFERENCES public.usuarios(id),
    nombre         VARCHAR(200) NOT NULL,
    telefono       VARCHAR(50),
    email          VARCHAR(150),
    direccion      VARCHAR(255),
    codigo_postal  VARCHAR(10),
    provincia      VARCHAR(100),
    pais           VARCHAR(100) DEFAULT 'España',
    dni            VARCHAR(50),
    fecha_creacion TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.materiales (
    id              BIGSERIAL PRIMARY KEY,
    usuario_id      BIGINT       NOT NULL REFERENCES public.usuarios(id),
    nombre          VARCHAR(200) NOT NULL,
    unidad_medida   VARCHAR(50)  DEFAULT 'ud',
    precio_unitario DOUBLE PRECISION NOT NULL DEFAULT 0,
    fecha_creacion  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.presupuestos (
    id                          BIGSERIAL PRIMARY KEY,
    usuario_id                  BIGINT           NOT NULL REFERENCES public.usuarios(id),
    cliente_id                  BIGINT           NOT NULL REFERENCES public.clientes(id),
    fecha_creacion              TIMESTAMP        DEFAULT NOW(),
    subtotal                    DOUBLE PRECISION NOT NULL DEFAULT 0,
    iva                         DOUBLE PRECISION NOT NULL DEFAULT 0,
    total                       DOUBLE PRECISION NOT NULL DEFAULT 0,
    iva_habilitado              BOOLEAN          NOT NULL DEFAULT TRUE,
    estado                      VARCHAR(50)      DEFAULT 'Pendiente',
    descuento_global_porcentaje DOUBLE PRECISION DEFAULT 0,
    descuento_global_fijo       DOUBLE PRECISION DEFAULT 0,
    descuento_antes_iva         BOOLEAN          DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS public.presupuesto_items (
    id              BIGSERIAL PRIMARY KEY,
    presupuesto_id  BIGINT           NOT NULL REFERENCES public.presupuestos(id),
    material_id     BIGINT           REFERENCES public.materiales(id),
    tarea_manual    VARCHAR(500),
    cantidad        DOUBLE PRECISION NOT NULL DEFAULT 1,
    precio_unitario DOUBLE PRECISION NOT NULL DEFAULT 0,
    subtotal        DOUBLE PRECISION NOT NULL DEFAULT 0,
    visible_pdf     BOOLEAN          DEFAULT TRUE,
    es_tarea_manual BOOLEAN          DEFAULT FALSE,
    aplica_iva      BOOLEAN          DEFAULT TRUE,
    descuento_porcentaje DOUBLE PRECISION DEFAULT 0,
    descuento_fijo  DOUBLE PRECISION DEFAULT 0
);

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

CREATE TABLE IF NOT EXISTS public.factura_items (
    id              BIGSERIAL PRIMARY KEY,
    factura_id      BIGINT           NOT NULL REFERENCES public.facturas(id),
    material_id     BIGINT           REFERENCES public.materiales(id),
    tarea_manual    VARCHAR(500),
    cantidad        DOUBLE PRECISION NOT NULL DEFAULT 1,
    precio_unitario DOUBLE PRECISION NOT NULL DEFAULT 0,
    subtotal        DOUBLE PRECISION NOT NULL DEFAULT 0,
    cuota_iva       DOUBLE PRECISION DEFAULT 0,
    es_tarea_manual BOOLEAN          DEFAULT FALSE,
    aplica_iva      BOOLEAN          DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS public.factura_secuencia (
    usuario_id    BIGINT      NOT NULL PRIMARY KEY REFERENCES public.usuarios(id),
    serie         VARCHAR(20) NOT NULL DEFAULT 'FAC',
    anio          INTEGER     NOT NULL,
    ultimo_numero INTEGER     NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS public.processed_stripe_events (
    id           BIGSERIAL PRIMARY KEY,
    event_id     VARCHAR(100) NOT NULL UNIQUE,
    processed_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_processed_stripe_events_event_id ON public.processed_stripe_events(event_id);
