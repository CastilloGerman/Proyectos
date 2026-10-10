# Despliegue de la API en Vercel (contenedor serverless)

Spike mínimo para ejecutar el backend Spring Boot como imagen de contenedor en Vercel y medir el arranque en frío.

## Estado actual

- **Dockerfile.vercel** en la raíz del proyecto (`AppGestion/Dockerfile.vercel`).
- **Perfil `serverless`** configurado en `application.yml`: Flyway desactivado, Hikari pool mínimo, jobs desactivados, modo de IP `vercel`.
- **Guard en `SubscriptionMigrationRunner`**: consulta `app_migration_state` antes de cargar todos los usuarios. Si falta la tabla, falla con un mensaje claro y accionable.
- **Resolución de IP unificada**: `TrustedClientAddressResolver` con tres modos configurables (`none`, `trusted-proxies`, `vercel`). El perfil `serverless` activa automáticamente el modo `vercel`.

## Resolución de IP del cliente

La propiedad `app.security.client-ip-mode` controla cómo se resuelve la IP del cliente:

| Modo | Comportamiento | Uso |
|---|---|---|
| `none` (default) | Ignora todas las cabeceras proxy; devuelve la IP peer. | Desarrollo local |
| `trusted-proxies` | Valida la IP peer contra `app.security.trusted-proxies` (lista CIDR). Solo si es confiable, lee `X-Forwarded-For`. | Detrás de un proxy con rangos IP conocidos |
| `vercel` | Usa `x-vercel-forwarded-for` (sobrevive si hay otro proxy delante de Vercel). Si falta, usa la última entrada de `X-Forwarded-For`. | Despliegue en Vercel |

Vercel **sobrescribe** `X-Forwarded-For` con la IP real del cliente (anti-spoofing). La cabecera `x-vercel-forwarded-for` es idéntica pero sobrevive si hay otro proxy delante de Vercel.

> **Seguridad**: en modo `vercel`, la API solo debe ser accesible a través de Vercel. Si usas un dominio personalizado (p. ej. `api.noemiweb.com`), el registro DNS en Cloudflare debe estar en **"solo DNS"** (DNS only, no proxied) para que Cloudflare no añada su propia capa de proxy y altere las cabeceras.

## Configuración del proyecto en Vercel

### Paso 1: Crear proyecto

1. En [vercel.com](https://vercel.com), crear un nuevo proyecto.
2. **Git Repository**: conectar el repositorio.
3. **Framework Preset**: `Other`.
4. **Root Directory**: `AppGestion` (la raíz del repo, donde está `Dockerfile.vercel`).
5. **Build Command**: dejar vacío (Vercel detecta `Dockerfile.vercel` automáticamente).
6. **Output Directory**: dejar vacío.

Vercel detecta `Dockerfile.vercel` en la raíz y construye la imagen OCI automáticamente.

### Paso 2: Variables de entorno

Configurar estas variables en **Settings → Environment Variables** del proyecto Vercel:

| Variable | Descripción | Perfil | Requerido |
|---|---|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC de Neon (pooler transaccional) | All | Sí |
| `PGUSER` | Usuario de la BD | All | Sí |
| `PGPASSWORD` | Contraseña de la BD | All | Sí |
| `JWT_SECRET` | Clave secreta JWT (mín. 32 caracteres) | All | Sí |
| `APP_EMAIL_TOKEN_SECRET` | Clave AES-256 para tokens OAuth email (mín. 32 chars) | All | Sí |
| `STRIPE_SECRET_KEY` | Clave secreta de Stripe | All | Sí |
| `STRIPE_WEBHOOK_SECRET` | Secret para verificar webhooks de Stripe | All | Sí |
| `STRIPE_PRICE_MONTHLY` | Price ID mensual de Stripe | All | Sí |
| `STRIPE_PRICE_YEARLY` | Price ID anual de Stripe | All | Sí |
| `FRONTEND_URL` | URL del frontend (ej. `https://noemiweb.com`) | All | Sí |
| `CORS_ALLOWED_ORIGINS` | Orígenes CORS permitidos | All | Sí |
| `RESEND_API_KEY` | API key de Resend para emails | All | Sí |
| `RESEND_WEBHOOK_SECRET` | Secret para webhooks de Resend | All | Sí |
| `GEMINI_API_KEY` | API key de Google Gemini | All | No |
| `APP_AI_GEMINI_ENABLED` | `true`/`false` para activar IA | All | No |

No es necesario configurar `APP_SECURITY_CLIENT_IP_MODE` ni `APP_SECURITY_TRUSTED_PROXIES`: el perfil `serverless` activa automáticamente `client-ip-mode=vercel`.

### Paso 3: Desplegar

Hacer un push a la rama conectada. Vercel construirá la imagen y desplegará automáticamente.

## Medir el arranque en frío

### Cómo medir

1. Desplegar y esperar a que la función se ejecute al menos una vez (para calentar).
2. Esperar **5 minutos** para que la función scale-down (en producción).
3. Hacer una petición a cualquier endpoint:
   ```bash
   curl -w "\n%{time_total}s\n" https://tu-proyecto.vercel.app/actuator/health
   ```
4. Revisar los logs de Vercel:
   ```bash
   vercel logs <deployment-url>
   ```
   Busca líneas como:
   - `Tomcat started on port` → indica que Spring Boot terminó de arrancar.
   - `Application appgestion-api is ready` → `ApplicationReadyEvent` disparado.

### Métricas a observar

| Métrica | Dónde verla | Umbral de alerta |
|---|---|---|
| Tiempo desde petición hasta respuesta | Logs de Vercel / `curl -w` | > 20 s |
| Memoria usada | Vercel Dashboard → Metrics | > 80% del límite |
| Duración de CPU activa | Vercel Dashboard → Metrics | Normal |

### Criterio de abandono

Si tras optimizaciones el **arranque en frío consistente > 20 segundos** o la **memoria insuficiente** causa OOM, se recomienda volver a un host siempre activo (Railway, Render, AWS ECS/Fargate).

## Migraciones de Flyway — antes del primer arranque

En el perfil `serverless`, Flyway está **desactivado**. Las migraciones deben ejecutarse **antes** del primer despliegue en Vercel, usando una conexión **directa** a Neon (sin pooler).

Si `SubscriptionMigrationRunner` detecta que falta la tabla `app_migration_state`, la aplicación fallará al arrancar con un mensaje claro indicando que se deben ejecutar las migraciones.

### Procedimiento exacto

1. **Construir el JAR** (localmente):
   ```powershell
   .\mvnw.cmd -pl api -Pprod package -DskipTests
   ```

2. **Ejecutar las migraciones** contra la BD de Neon (conexión directa, no pooler):
   ```powershell
   $env:SPRING_DATASOURCE_URL="jdbc:postgresql://ep-xxx.us-east-1.aws.neon.tech/appgestion?options=project%3Dyour-project"
   $env:PGUSER="your-user"
   $env:PGPASSWORD="your-password"
   $env:SPRING_PROFILES_ACTIVE="prod"
   java -jar api\target\appgestion-api.jar &
   # Esperar a que aparezca "Application appgestion-api is ready" en los logs
   # Ctrl+C para detener
   ```

   Alternativamente, usar el plugin de Maven (si está configurado en el pom):
   ```powershell
   .\mvnw.cmd -pl api flyway:migrate `
     -Dflyway.url="jdbc:postgresql://ep-xxx.us-east-1.aws.neon.tech/appgestion?options=project%3Dyour-project" `
     -Dflyway.user="your-user" `
     -Dflyway.password="your-password"
   ```

3. **Verificar** que las 45 migraciones se aplicaron (V1..V45):
   ```sql
   SELECT version, success FROM flyway_schema_ordering ORDER BY installed_rank;
   ```

4. **Detener** el proceso (Ctrl+C).

5. **Desplegar en Vercel** con `SPRING_PROFILES_ACTIVE=prod,serverless`. La API validará el schema pero no ejecutará migraciones.

> **Importante**: usa la URL de conexión **directa** de Neon (no el pooler) para ejecutar Flyway. El pooler transaccional puede causar problemas con las tablas temporales que Flyway crea durante las migraciones.

## Jobs programados (@Scheduled)

En serverless, **los `@Scheduled` no disparan de forma fiable**: no hay proceso persistente. Los jobs están desactivados en el perfil `serverless`:

| Job | Estado en serverless | Plan |
|---|---|---|
| `UsuarioSesionCleanupJob` | Desactivado | Endpoint croneable (fase siguiente) |
| `AuditAccessCleanupJob` | Desactivado | Endpoint croneable (fase siguiente) |
| `TrialExpirationJob` | Desactivado | Endpoint croneable (fase siguiente) |
| `FacturaRecordatorioJob` | Desactivado | Endpoint croneable (fase siguiente) |
| `PresupuestoSeguimientoJob` | Desactivado | Endpoint croneable (fase siguiente) |
| `OauthPendingCleanup` | Desactivado | Endpoint croneable (fase siguiente) |
| `EmailJobWorker` | Desactivado | Rediseño necesario (fase siguiente) |

**Importante**: este spike NO implementa los endpoints croneables. Se documenta el comportamiento para implementar en la fase siguiente.

## Rate limiters en memoria

Los rate limiters (`AiRequestRateLimiter`, `AiDailyRequestRateLimiter`, `AiProviderAttemptRateLimiter`, `PublicLinkRateLimiter`, `AuthRateLimitFilter`) usan memoria local. Con múltiples instancias serverless, los límites se relajan proporcionalmente al número de instancias activas.

**Este spike NO migra los rate limiters a estado compartido**. Para un solo usuario/instancia, el comportamiento es correcto. En la fase siguiente, se migrarán a tablas en PostgreSQL.

## Archivos creados/modificados

| Archivo | Cambio |
|---|---|
| `Dockerfile.vercel` | **Creado**: multi-stage, Java 21 JRE, usuario no root, puerto `$PORT` (default 80) |
| `.dockerignore` | **Creado**: excluye archivos innecesarios del contexto |
| `api/.../AppMigrationState.java` | **Creado**: entidad para flags de migración |
| `api/.../AppMigrationStateRepository.java` | **Creado**: repositorio JPA |
| `api/.../V45__app_migration_state.sql` | **Creado**: migración Flyway |
| `api/.../SubscriptionMigrationRunner.java` | **Modificado**: guard con flag en BD; falla con mensaje claro si falta la tabla |
| `api/.../TrustedClientAddressResolver.java` | **Modificado**: tres modos (none, trusted-proxies, vercel) |
| `api/.../PublicClientAddressResolver.java` | **Modificado**: delega en TrustedClientAddressResolver unificado |
| `api/.../AuthRateLimitFilter.java` | **Modificado**: usa ClientIpMode |
| `api/.../ClientMetadataParser.java` | **Modificado**: acepta ClientIpMode |
| `api/.../SessionService.java` | **Modificado**: inyecta y pasa ClientIpMode |
| `api/.../AuditAccessService.java` | **Modificado**: inyecta y pasa ClientIpMode |
| `api/.../SecurityConfig.java` | **Modificado**: inyecta y pasa ClientIpMode al filtro |
| `api/.../application.yml` | **Modificado**: app.security.client-ip-mode + perfil serverless con vercel |
| `api/.../SubscriptionMigrationRunnerTest.java` | **Modificado**: test para tabla faltante |
| `api/.../TrustedClientAddressResolverTest.java` | **Modificado**: tests para los tres modos |
| `api/.../PublicClientAddressResolverTest.java` | **Modificado**: tests con Mockito |

## Limitaciones conocidas

1. **EmailJobWorker**: no funciona en serverless (polling cada 3 s). Los emails no se enviarán hasta implementar procesamiento reactivo o cola externa.
2. **Rate limiters**: ineficaces con múltiples instancias concurrentes.
3. **Jobs programados**: desactivados. Sin endpoints croneables, las tareas de limpieza no se ejecutan.
4. **Caches Caffeine**: cada instancia tiene su caché local; datos inconsistentes entre instancias (impacto bajo para `MATERIALES_TOP_USADOS`).
