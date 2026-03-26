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

## 3. Seguridad y hardening pre-producción

Revisión orientativa antes del go-live (complementa §1–2). No sustituye un pentest ni la configuración concreta de tu proxy.

### Spring Security y API REST

- La API usa **sesión stateless** y **CSRF desactivado** de forma habitual en REST con JWT en la cabecera `Authorization`. El riesgo equivalente pasa por **XSS** en el frontend y robo del token; mitígalo con **HTTPS**, políticas de contenido (**CSP**) donde puedas, y el mismo cuidado descrito en §5 sobre `localStorage`.
- **Perfil `local`:** solo en máquinas de desarrollo. En ese perfil el backend puede usar CORS muy permisivo (p. ej. patrones tipo `http://*:4200` para probar desde la LAN). **Nunca** despliegues con `SPRING_PROFILES_ACTIVE=local` en un servidor accesible desde Internet.

### JWT

- El token viaja en texto si la conexión no va cifrada. En producción, sirve **API y frontend solo por HTTPS** (certificados válidos en el proxy o terminación TLS).
- **Mejora opcional (código):** validar explícitamente el claim `iss` (issuer) en el backend si en el futuro compartes infraestructura o rotas emisores; no es requisito documental para el primer despliegue si el secreto es único por entorno.

### CORS

- Revisa que `CORS_ALLOWED_ORIGINS` liste **exactamente** los orígenes públicos del SPA (incluida la variante **con o sin `www`** si ambas existen).
- No uses comodines amplios en producción; la lista explícita es la contrapartida de usar credenciales en peticiones cross-origin.

### Actuator (Spring Boot)

- La API incluye `spring-boot-starter-actuator`. **Antes de producción**, confirma en el entorno real qué endpoints HTTP expone Spring Boot 4 con la configuración por defecto (no hay bloque `management.*` fijado en el YAML del repo).
- La seguridad HTTP de la aplicación puede exigir **autenticación** también para rutas bajo `/actuator`. Si usas **Kubernetes u otro orquestador** con *health checks* anónimos a `/actuator/health`, puede devolverse **401** hasta que definas una estrategia: por ejemplo `permitAll` solo para `GET /actuator/health`, un **puerto de gestión** separado, o *probes* que envíen credenciales. Documenta la opción elegida cuando la implementes.

---

## 4. Base de datos (PostgreSQL)

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

## 5. Frontend (Angular)

- El proyecto usa **Angular 21.x** en dependencias (`package.json`); las guías genéricas “Angular 20” pueden diferir en detalles de CLI.
- **npm en CI y en el servidor de build:** si el repositorio incluye [`frontend/.npmrc`](c:\Users\German\Documents\Proyectos\AppGestion\frontend\.npmrc) con `legacy-peer-deps=true` (p. ej. para alinear TypeScript 6.x con peers de Angular), **respétalo** al ejecutar `npm ci` o `npm install` en el pipeline; no lo sustituyas por flags distintos sin revisar resolución de dependencias.
- La base de la API se define en `environment.ts` / `environment.prod.ts` como **`apiUrl`**:
  - **`/api`** (recomendado): el navegador llama a la misma origen del front + `/api/...`; en desarrollo, `proxy.conf.js` reenvía `/api` al backend (puerto 8081) sin CORS extra. En producción, nginx (u otro proxy) debe hacer lo mismo o cambia `apiUrl` a `''` si la API está en el mismo host sin prefijo.
  - **`''` (vacío)**: peticiones relativas `/auth`, `/facturas`, etc., solo válidas si el front y la API comparten exactamente el mismo origen.
- Build típico: `ng build --configuration production` (sustituye `environment.ts` por `environment.prod.ts`). Si el build advierte dependencias **CommonJS** (p. ej. `qrcode` en TOTP), es esperable en muchos paquetes; el QR se carga de forma diferida y no bloquea el despliegue. Solo si necesitas silenciar el aviso en CI, valora `allowedCommonJsDependencies` en `angular.json` (sin cambiar comportamiento en runtime).
- Sirve los estáticos detrás de **HTTPS**.
- El JWT en `localStorage` es habitual pero sensible a **XSS**; a medio plazo valorar cookies **httpOnly** + refresh token y **CSP** en el hosting del front.
- Los formularios de factura/presupuesto usan tipos alineados con `FacturaRequest` / filas de `FormGroup` donde aplica (revisión continua en nuevas pantallas).

---

## 6. Infraestructura recomendable

- **HTTPS** en API y frontend.
- **Rate limiting** en rutas de autenticación (login, registro, recuperación, etc.) vía API Gateway, reverse proxy o la propia API.
- Logs centralizados y alertas básicas (errores 5xx, fallos de pago/webhook).
- Tras proxy: **IP del cliente** correcta para `usuario_sesion` (ver sección de variables “Sesiones por dispositivo”).
- Con perfil **prod**, la API limita el detalle de errores HTTP al cliente (`server.error`); tras desplegar, verifica que las respuestas de error no expongan datos internos (coherente con §9).

---

## 7. Webhooks Stripe

- En el dashboard Stripe, apunta el webhook a tu URL pública (ej. `https://api.tudominio.com/webhook/stripe`).
- Usa el **signing secret** (`whsec_...`) que coincida con ese endpoint en `STRIPE_WEBHOOK_SECRET`.

---

## 8. Checklist antes de publicar

- [ ] `SPRING_PROFILES_ACTIVE=prod`
- [ ] `JWT_SECRET` fuerte y único (solo en secretos del entorno, no en Git)
- [ ] Stripe: claves y webhook secret de producción (o entorno de test consciente)
- [ ] `CORS_ALLOWED_ORIGINS` = dominio(s) exactos del frontend (sin comodines de LAN; revisar `www` vs apex)
- [ ] CORS sin perfil `local` en el servidor; orígenes alineados con la URL pública del SPA
- [ ] HTTPS en front y API (certificados válidos)
- [ ] Actuator: endpoints expuestos revisados en el entorno real; estrategia de health/liveness definida si hay balanceador u orquestador
- [ ] BD migrada con Flyway y backup realizado (incluye tabla `usuario_sesion` si tu versión la incorpora)
- [ ] (Opcional) `SESSIONS_CLEANUP_ENABLED` / `SESSIONS_CLEANUP_RETENTION_DAYS` revisados
- [ ] SMTP configurado si usas envío de correo
- [ ] Frontend build apuntando a la URL de la API de producción
- [ ] Ningún perfil `local` ni endpoints de desarrollo expuestos en el servidor
- [ ] (Opcional) Nivel de log en prod sin `DEBUG` en paquetes que impriman datos sensibles

---

## 9. Después del despliegue

- Probar: login, flujo principal (clientes / presupuestos / facturas), PDF, flujo de pago Stripe y recepción de webhook.
- Comprobar que los errores al cliente no expongan stack traces ni datos internos.

---

## 10. Desarrollo local (recordatorio)

- Suele usarse el perfil **`local`** (p. ej. `spring-boot:run` con perfil definido en el `pom.xml` o script `iniciar-api.ps1`).
- Ahí pueden existir valores por defecto solo para desarrollo; **no** reutilizarlos en producción.

---

*Documento orientativo; ajusta nombres de variables si tu `application.yml` difiere en una rama concreta.*
