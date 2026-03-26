# Despliegue — AppGestion

Guía de recordatorio para poner la API y el frontend en producción. **No incluye secretos reales**: los valores se definen solo en el servidor, CI o un gestor de secretos.

---

## Qué sí / qué no subir a Git

| Contenido | ¿En el repo? |
|-----------|----------------|
| Este archivo (`docs/DEPLOY.md`) y checklists sin credenciales | Sí |
| `.env.example` solo con **nombres** de variables (sin valores) | Sí |
| Archivos `.env`, claves Stripe/JWT reales, contraseñas de BD | **No** (añádelos a `.gitignore`) |

Si el repositorio es público o se comparte, asume que cualquier secreto en Git puede filtrarse.

---

## 1. Perfil Spring (API)

- **Producción:** `SPRING_PROFILES_ACTIVE=prod`
- Con **prod**: Hibernate `ddl-auto=validate` — el esquema lo gestiona **Flyway**, no actualizaciones automáticas de Hibernate.
- **No** uses el perfil `local` en servidores reales (desarrollo: omite comprobaciones pensadas solo para dev).

---

## 2. Variables de entorno obligatorias (API)

### Críticas

- **`JWT_SECRET`**: cadena larga y aleatoria (**≥ 32 caracteres** para HS256). Sin un secreto fuerte, cualquiera que lo conozca puede falsificar tokens.
- **Stripe (modo producción):**
  - `STRIPE_SECRET_KEY`
  - `STRIPE_WEBHOOK_SECRET`
  - En `prod` la aplicación valida que no sean placeholders; usa claves reales del dashboard Stripe.
- **`STRIPE_PRICE_MONTHLY`** (y URLs de éxito/cancelación del checkout si las usas) según tu integración.

### Base de datos

- `SPRING_DATASOURCE_URL` (si no usas el JDBC por defecto del `application.yml`)
- `DB_USERNAME`, `DB_PASSWORD`

### CORS

- **`CORS_ALLOWED_ORIGINS`**: dominios del frontend, **separados por coma** (ej. `https://app.tudominio.com`).
- Con credenciales en CORS no puede usarse `*`; debe ser una lista explícita.

### Correo (opcional pero necesario si envías PDFs o recuperación de contraseña)

- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`

### Otros

- `FRONTEND_URL` — base del SPA para enlaces en emails (reset password, etc.)

### Sesiones por dispositivo (tabla `usuario_sesion`)

- No suelen requerir variables extra: Flyway crea la tabla y la API registra IP, navegador y tipo de dispositivo en cada login.
- **Limpieza automática:** cada noche (03:00, hora del servidor) se borran filas cuyo `expires_at` es **anterior a hoy menos N días** (por defecto **30**). Así la tabla no crece sin límite.
  - `SESSIONS_CLEANUP_ENABLED` — `true` (defecto) o `false` para desactivar el job.
  - `SESSIONS_CLEANUP_RETENTION_DAYS` — días de retención tras la caducidad del token (mínimo efectivo en código: 1).
- **IP real del cliente:** si la API va detrás de un reverse proxy o balanceador, configura cabeceras estándar (`X-Forwarded-For`, `Forwarded`, etc.) y que Spring/Tomcat reconozcan IPs de confianza; si no, la IP guardada puede ser la del proxy.

---

## 3. Base de datos (PostgreSQL)

- Se recomienda **PostgreSQL 17** (LTS actual del proyecto en documentación); versiones anteriores pueden funcionar si el driver y las migraciones son compatibles.
- La API debe poder conectar a PostgreSQL desde el entorno de ejecución.
- Ejecuta las migraciones **Flyway** antes o en el arranque coherente con tu estrategia; con `validate`, el esquema debe coincidir con las migraciones aplicadas.
- Haz **backup** de la BD antes de migrar en producción.
- Incluye la migración que crea **`usuario_sesion`** (sesiones activas / dispositivos): sin ella el arranque fallará en `validate` cuando el código espere esa tabla.

### Pasos nuevos al actualizar desde una versión sin sesiones por dispositivo

1. **Despliega la API** con el código que incluye `usuario_sesion` y el job de limpieza (o aplica solo migraciones primero, según tu proceso).
2. **Aplica migraciones Flyway** en el entorno (la nueva versión añade `V19__usuario_sesion.sql` o la que corresponda en tu rama).
3. **Reinicia** la API con el mismo `JWT_SECRET` y variables de siempre; no hace falta rotar JWT por este cambio.
4. Los usuarios con tokens **antiguos sin claim `sid`** seguirán entrando hasta que caduquen; al **volver a iniciar sesión** tendrán sesión registrada y podrán usar “Sesiones activas” con datos completos.
5. (Opcional) Ajusta `SESSIONS_CLEANUP_RETENTION_DAYS` o `SESSIONS_CLEANUP_ENABLED` si tu política de retención o auditoría lo exige.

---

## 4. Frontend (Angular)

- La base de la API se define en `environment.ts` / `environment.prod.ts` como **`apiUrl`**:
  - **`/api`** (recomendado): el navegador llama a la misma origen del front + `/api/...`; en desarrollo, `proxy.conf.js` reenvía `/api` al backend (puerto 8081) sin CORS extra. En producción, nginx (u otro proxy) debe hacer lo mismo o cambia `apiUrl` a `''` si la API está en el mismo host sin prefijo.
  - **`''` (vacío)**: peticiones relativas `/auth`, `/facturas`, etc., solo válidas si el front y la API comparten exactamente el mismo origen.
- Build típico: `ng build --configuration production` (sustituye `environment.ts` por `environment.prod.ts`).
- Sirve los estáticos detrás de **HTTPS**.
- El JWT en `localStorage` es habitual pero sensible a **XSS**; a medio plazo valorar cookies **httpOnly** + refresh token y **CSP** en el hosting del front.

---

## 5. Infraestructura recomendable

- **HTTPS** en API y frontend.
- **Rate limiting** en rutas de autenticación (login, registro, recuperación, etc.) vía API Gateway, reverse proxy o la propia API.
- Logs centralizados y alertas básicas (errores 5xx, fallos de pago/webhook).
- Tras proxy: **IP del cliente** correcta para `usuario_sesion` (ver sección de variables “Sesiones por dispositivo”).

---

## 6. Webhooks Stripe

- En el dashboard Stripe, apunta el webhook a tu URL pública (ej. `https://api.tudominio.com/webhook/stripe`).
- Usa el **signing secret** (`whsec_...`) que coincida con ese endpoint en `STRIPE_WEBHOOK_SECRET`.

---

## 7. Checklist antes de publicar

- [ ] `SPRING_PROFILES_ACTIVE=prod`
- [ ] `JWT_SECRET` fuerte y único (solo en secretos del entorno, no en Git)
- [ ] Stripe: claves y webhook secret de producción (o entorno de test consciente)
- [ ] `CORS_ALLOWED_ORIGINS` = dominio(s) exactos del frontend
- [ ] BD migrada con Flyway y backup realizado (incluye tabla `usuario_sesion` si tu versión la incorpora)
- [ ] (Opcional) `SESSIONS_CLEANUP_ENABLED` / `SESSIONS_CLEANUP_RETENTION_DAYS` revisados
- [ ] SMTP configurado si usas envío de correo
- [ ] Frontend build apuntando a la URL de la API de producción
- [ ] Ningún perfil `local` ni endpoints de desarrollo expuestos en el servidor

---

## 8. Después del despliegue

- Probar: login, flujo principal (clientes / presupuestos / facturas), PDF, flujo de pago Stripe y recepción de webhook.
- Comprobar que los errores al cliente no expongan stack traces ni datos internos.

---

## 9. Desarrollo local (recordatorio)

- Suele usarse el perfil **`local`** (p. ej. `spring-boot:run` con perfil definido en el `pom.xml` o script `iniciar-api.ps1`).
- Ahí pueden existir valores por defecto solo para desarrollo; **no** reutilizarlos en producción.

---

*Documento orientativo; ajusta nombres de variables si tu `application.yml` difiere en una rama concreta.*
