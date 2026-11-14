# Documentación

Índice de los ficheros de `docs/`.

- [DB-AUDIT-INTENTIONAL-NULLABLES.md](DB-AUDIT-INTENTIONAL-NULLABLES.md) — Columnas nullable a propósito: deben etiquetarse y no proponerse como `NOT NULL`.
- [DB-UNUSED-COLUMNS.md](DB-UNUSED-COLUMNS.md) — `presupuestos.foto_trabajo` y `presupuestos.firma_cliente`, columnas `BYTEA` sin uso en el código actual.
- [DEPENDENCIES.md](DEPENDENCIES.md) — Versiones de referencia de la API (Maven / Spring Boot) y del frontend (Node).
- [DEPLOY.md](DEPLOY.md) — Guía de despliegue a producción de la API y el frontend, sin secretos reales.
- [DESPLIEGUE-VERCEL-API.md](DESPLIEGUE-VERCEL-API.md) — Spike para ejecutar el backend Spring Boot como contenedor serverless en Vercel.
- [EMAIL-OAUTH-SETUP.md](EMAIL-OAUTH-SETUP.md) — Configuración OAuth de Gmail y Microsoft/Outlook para enviar correo en nombre del usuario.
- [ENLACE-PUBLICO.md](ENLACE-PUBLICO.md) — Modelo de amenazas, endpoints y datos expuestos del enlace público de presupuestos.
- [IA-PRESUPUESTOS.md](IA-PRESUPUESTOS.md) — Borradores de presupuesto con IA: transcribe partidas y precios dictados, sin persistirlos.
- [RESPUESTA-CLIENTE.md](RESPUESTA-CLIENTE.md) — Aviso «Me interesa» o «Tengo dudas» desde el enlace público, sin cambiar el estado del presupuesto.
- [SEGUIMIENTO-PRESUPUESTOS.md](SEGUIMIENTO-PRESUPUESTOS.md) — Avisos automáticos diarios al propietario de presupuestos enviados y pendientes.
- [TENANT-MODEL.md](TENANT-MODEL.md) — Modelo multi-tenant por organización y aislamiento actual de los datos por `usuario_id`.
- [TROUBLESHOOTING-PASSWORD-RESET.md](TROUBLESHOOTING-PASSWORD-RESET.md) — Diagnóstico de por qué no llega el correo de recuperación de contraseña.
- [db-audit-part2-results.txt](db-audit-part2-results.txt) — Salida de la auditoría PostgreSQL del 2026-04-04 (tablas, claves foráneas, huérfanos y nullables candidatos).
- [db-audit-part4-nullable-summary.txt](db-audit-part4-nullable-summary.txt) — Resumen de la sección 6: 117 columnas nullable candidatas a revisión, con las exclusiones intencionales.
- [db-explain-part4-results.txt](db-explain-part4-results.txt) — Planes `EXPLAIN (ANALYZE, BUFFERS)` del 2026-04-04 sobre `appgestion` (PostgreSQL 17).
- [reparto-descuento-ficheros.md](reparto-descuento-ficheros.md) — Ficheros y fragmentos del commit `cad694c` que pertenecen al reparto de descuento.
