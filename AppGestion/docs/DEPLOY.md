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

## Lanzamiento a producción (pasos ordenados)

Secuencia orientativa para el **primer go-live**. Los detalles de cada variable y el checklist final están en las secciones **§2**, **§8** y **§9**.

1. **Dominio y HTTPS**  
   Define la URL pública del **frontend** (SPA) y la de la **API** (mismo host con ruta `/api` o subdominio tipo `api.tudominio.com`). Activa **HTTPS** en el proxy, CDN o PaaS (certificados válidos).

2. **Base de datos**  
   Crea la instancia **PostgreSQL** (versión alineada con la documentación del proyecto). Reserva `SPRING_DATASOURCE_URL`, `DB_USERNAME` y `DB_PASSWORD` solo en el entorno de ejecución. Planifica **backup** antes de aplicar migraciones en producción.

3. **Correo transaccional (Resend)**  
   En [Resend](https://resend.com), **verifica un dominio** que controlas y añade en DNS los registros que indiquen (SPF, DKIM, etc.). Genera `RESEND_API_KEY` y configúrala en secretos del servidor.  
   - Configura **`APP_EMAIL_SYSTEM_FROM`** con un remitente de ese dominio (p. ej. `Noemí <noreply@tudominio.com>`). El remitente por defecto `onboarding@resend.dev` no sirve para entregar a clientes reales de forma fiable.  
   - **Importante:** sin dominio verificado, Resend en modo de prueba solo permite enviar al correo asociado a la cuenta; recuperación de contraseña, facturas y demás correos a usuarios finales requieren dominio verificado en producción.

4. **Stripe**  
   Cuando pases a cobros reales, usa claves **live** en el dashboard: `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, `STRIPE_PRICE_MONTHLY` y URLs de éxito, cancelación y portal del cliente apuntando a `https://tu-dominio-front/...` (ver `application.yml` / variables `STRIPE_*_URL`).

5. **Variables de entorno de la API**  
   Fija al menos: `SPRING_PROFILES_ACTIVE=prod`, `JWT_SECRET` (≥ 32 caracteres, aleatorio), `CORS_ALLOWED_ORIGINS` (orígenes exactos del SPA, separados por coma), `FRONTEND_URL` (base del SPA para enlaces en correos y OAuth de correo), base de datos, Resend y Stripe. Lista completa en **§2**.  
   Si las organizaciones usarán **Gmail u Outlook** desde la app: `APP_EMAIL_TOKEN_SECRET` y credenciales OAuth con redirect URI `https://tu-api/.../auth/email/oauth/.../callback` — guía en [EMAIL-OAUTH-SETUP.md](EMAIL-OAUTH-SETUP.md).

6. **Inicio de sesión con Google (botón en el login del SPA)**  
   En Google Cloud Console → credenciales del cliente OAuth usado por el frontend, añade en **Orígenes de JavaScript autorizados** la URL exacta del SPA en producción (incluido esquema y puerto si aplica). Sin esto, el navegador mostrará errores de tipo *origin is not allowed*.

7. **Migraciones Flyway**  
   Asegura que el esquema esté al día antes o durante el primer arranque con perfil `prod` (`ddl-auto=validate`). Haz un **backup** de la BD antes de migrar en un entorno ya en uso.

8. **Desplegar la API**  
   Ejecuta la API con el conjunto de variables de producción. **No** uses `SPRING_PROFILES_ACTIVE=local` en servidores expuestos a Internet.

9. **Construir y desplegar el frontend**  
   `ng build --configuration production` (sustituye `environment.ts` por `environment.prod.ts`). Sirve los estáticos detrás de HTTPS. Revisa que `apiUrl` en producción apunte correctamente a la API (mismo origen + `/api` o URL absoluta, según tu proxy).

10. **Webhook de Stripe**  
    En el dashboard de Stripe, registra `https://tu-api-publica/webhook/stripe` y el **signing secret** que coincida con `STRIPE_WEBHOOK_SECRET`.

11. **Comprobaciones finales**  
    Sigue el **checklist de §8**, las pruebas de **§9** y, si aplica, revisa en base de datos la tabla `email_jobs` (estado `SENT` vs `DEAD` y `last_error`) si algún correo no llega.

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

### Correo (envío transaccional y cola)

La API encola el correo en base de datos y un worker envía según la configuración de cada organización. En **modo por defecto (system)** se usa el proveedor **Resend** vía HTTP.

- **`RESEND_API_KEY`** — obligatoria en producción si quieres que salgan correos (facturas, PDFs, recuperación de contraseña, soporte, etc.). Define el valor **solo** en el servidor, CI o gestor de secretos; **nunca** en Git.  
  - Equivalente en YAML: `app.email.resend.api-key` (solo si inyectas configuración por archivo en el servidor, no en el repo).
- **`APP_EMAIL_SYSTEM_FROM`** (opcional) — remitente por defecto del modo system, en formato `Nombre <correo@dominio-verificado>`. Debe coincidir con un dominio **verificado** en Resend.  
  - Equivalente: `app.email.system-from`.
- **`APP_EMAIL_TOKEN_SECRET`** — obligatoria si las organizaciones **conectan Gmail u Outlook (OAuth)**; cifra tokens en reposo. Cadena larga y aleatoria (≥ 32 caracteres recomendado).  
  - Equivalente: `app.email.token-encryption-key`.
- **OAuth (solo si usas “Conectar Gmail / Microsoft” en datos de empresa):** `GOOGLE_OAUTH_CLIENT_ID`, `GOOGLE_OAUTH_CLIENT_SECRET`, `GOOGLE_OAUTH_REDIRECT_URI`, y/o análogas de Microsoft (`MICROSOFT_OAUTH_*`), alineadas con las URLs públicas de tu API. Guía paso a paso en [EMAIL-OAUTH-SETUP.md](EMAIL-OAUTH-SETUP.md).
- **Webhook Resend (opcional, deliverability):** `RESEND_WEBHOOK_SECRET` si validas firmas en el backend; el endpoint `POST /webhook/resend` persiste eventos para análisis.

**DNS (operación):** en el dominio de envío configura **SPF**, **DKIM** y **DMARC** según la documentación de Resend; no son variables de la app pero son necesarios para buena entregabilidad.

**SMTP clásico (`MAIL_*`)** solo aplica si en alguna organización sigues usando el modo **SMTP manual** heredado; el flujo por defecto no depende de `spring.mail` para el envío de negocio.

### Otros

- `FRONTEND_URL` — base del SPA para enlaces en emails (reset password, etc.) y redirecciones tras OAuth de correo

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

Complemento de la sección **Lanzamiento a producción (pasos ordenados)**; úsalo como lista de cierre antes de abrir tráfico real.

- [ ] `SPRING_PROFILES_ACTIVE=prod`
- [ ] `JWT_SECRET` fuerte y único (solo en secretos del entorno, no en Git)
- [ ] Stripe: claves y webhook secret de producción (o entorno de test consciente)
- [ ] `CORS_ALLOWED_ORIGINS` = dominio(s) exactos del frontend (sin comodines de LAN; revisar `www` vs apex)
- [ ] CORS sin perfil `local` en el servidor; orígenes alineados con la URL pública del SPA
- [ ] HTTPS en front y API (certificados válidos)
- [ ] Actuator: endpoints expuestos revisados en el entorno real; estrategia de health/liveness definida si hay balanceador u orquestador
- [ ] BD migrada con Flyway y backup realizado (incluye tabla `usuario_sesion` si tu versión la incorpora)
- [ ] (Opcional) `SESSIONS_CLEANUP_ENABLED` / `SESSIONS_CLEANUP_RETENTION_DAYS` revisados
- [ ] **Resend:** `RESEND_API_KEY` en secretos del entorno; remitente `APP_EMAIL_SYSTEM_FROM` alineado con dominio verificado en Resend
- [ ] (Si OAuth de correo) `APP_EMAIL_TOKEN_SECRET` y credenciales OAuth de Google/Microsoft con redirect URIs a `https://tu-api/.../auth/email/oauth/.../callback`
- [ ] (Opcional) DNS del dominio de envío: SPF / DKIM / DMARC (Resend)
- [ ] (Solo si alguna org sigue en SMTP legacy) `MAIL_*` o credenciales SMTP en servidor
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
