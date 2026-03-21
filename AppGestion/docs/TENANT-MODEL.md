# Modelo multi-tenant (organización)

## Estado actual (tras V9 + código)

- **`organizations`**: workspace B2B (nombre, `created_at`).
- **`organization_members`**: relación usuario ↔ organización con `role` (p. ej. `OWNER`, `OPERARIO`).
- **`usuarios.organization_id`**: NOT NULL; cada usuario pertenece a una organización.
- **Alta por registro / Google (nuevo usuario)**: se crea organización **"Personal"**, membresía `OWNER` y se asigna al usuario.
- **Alta por enlace de referido (correo)**: el referido crea **su propia** organización "Personal" (cuenta independiente). Se guarda `referred_by_usuario_id` en `usuarios` para trazabilidad. Los permisos de edición los marca la **suscripción / prueba** (`canWrite`), no un rol en el enlace.

## Aislamiento de datos hoy

Las tablas de negocio (`clientes`, `facturas`, `presupuestos`, etc.) siguen filtrando por **`usuario_id`**.  
Compartir datos entre miembros de la misma organización requerirá una fase posterior:

1. Añadir `organization_id` (o equivalente) a entidades de negocio.
2. Migrar datos existentes desde el `usuario_id` “propietario” o política acordada.
3. Sustituir o complementar consultas `findByUsuarioId` por `findByOrganizationId` + reglas de rol.
4. Opcional: Row Level Security en PostgreSQL por `organization_id`.

## Migración Flyway

- **`V9__organizations_and_memberships.sql`**: tablas, índices, columna en `usuarios`, backfill con bloque `DO` para usuarios existentes, y `NOT NULL` en `organization_id`.
- **`V10__usuario_referred_by.sql`**: `referred_by_usuario_id` opcional en `usuarios`.
