-- Índices de apoyo a FKs y consultas frecuentes (auditoría BD Parte 2).
-- CREATE INDEX IF NOT EXISTS: idempotente si se re-ejecuta en entornos con drift manual.

CREATE INDEX IF NOT EXISTS idx_clientes_usuario_id ON public.clientes (usuario_id);
CREATE INDEX IF NOT EXISTS idx_materiales_usuario_id ON public.materiales (usuario_id);

CREATE INDEX IF NOT EXISTS idx_presupuestos_usuario_fecha
    ON public.presupuestos (usuario_id, fecha_creacion DESC);
CREATE INDEX IF NOT EXISTS idx_presupuestos_usuario_cliente_fecha
    ON public.presupuestos (usuario_id, cliente_id, fecha_creacion DESC);

CREATE INDEX IF NOT EXISTS idx_presupuesto_items_presupuesto_id ON public.presupuesto_items (presupuesto_id);
CREATE INDEX IF NOT EXISTS idx_presupuesto_items_material_id ON public.presupuesto_items (material_id)
    WHERE material_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_factura_items_factura_id ON public.factura_items (factura_id);
CREATE INDEX IF NOT EXISTS idx_factura_items_material_id ON public.factura_items (material_id)
    WHERE material_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_facturas_usuario_cliente_fecha
    ON public.facturas (usuario_id, cliente_id, fecha_creacion DESC);
CREATE INDEX IF NOT EXISTS idx_facturas_presupuesto_id ON public.facturas (presupuesto_id)
    WHERE presupuesto_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_facturas_factura_anticipo_id ON public.facturas (factura_anticipo_id)
    WHERE factura_anticipo_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_invitaciones_inviter_usuario_id ON public.invitaciones (inviter_usuario_id);
CREATE INDEX IF NOT EXISTS idx_email_jobs_usuario_id ON public.email_jobs (usuario_id);
CREATE INDEX IF NOT EXISTS idx_oauth_pending_usuario_id ON public.oauth_pending (usuario_id);
CREATE INDEX IF NOT EXISTS idx_usuarios_organization_id ON public.usuarios (organization_id);

-- Job de recordatorios: filtra por vencimiento, impagadas, no anuladas, sin recordatorio enviado.
CREATE INDEX IF NOT EXISTS idx_facturas_recordatorio_job
    ON public.facturas (fecha_vencimiento)
    WHERE anulada = false
      AND estado_pago <> 'Pagada'
      AND fecha_vencimiento IS NOT NULL
      AND recordatorio_enviado IS NOT TRUE;
