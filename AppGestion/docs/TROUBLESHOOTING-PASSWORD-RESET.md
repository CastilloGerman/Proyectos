# Recuperación de contraseña: por qué no llega el correo

La pantalla muestra un mensaje neutro **también cuando la solicitud HTTP tiene éxito**, aunque el backend no envíe correo si el email no está registrado (anti-enumeración). Usa esta guía para diagnosticar.

## 0. Muy frecuente: Resend en modo prueba (error 403 en logs)

Si **`RESEND_API_KEY`** está bien pero el correo **no llega**, mira la consola de la API al procesar `email_jobs`. A menudo aparece:

```text
email_dispatch_failed provider=resend reason=403 Forbidden: ... "validation_error" ... "You can only send testing emails to your own email address (...). To send emails to other recipients, please verify a domain at resend.com/domains ..."
```

**Qué significa:** con la API key de Resend en **modo de prueba** (sin dominio verificado), Resend **solo acepta envíos al mismo correo** que usaste al registrarte en Resend. Si el usuario de la app tiene otro email (Hotmail, otro Gmail, etc.), **Resend rechaza el envío**; el enlace no llega aunque la app encole el trabajo bien.

**Qué puedes hacer:**

1. **Desarrollo / pruebas:** pide recuperación con una cuenta cuyo `usuarios.email` sea **exactamente** el correo permitido por tu cuenta Resend en modo prueba, **o** usa el enlace que la API escribe en log con perfil `local` (busca `enlace de recuperación de contraseña`).
2. **Uso real:** en [Resend](https://resend.com/domains) **verifica un dominio**, configura DNS (SPF/DKIM) y define **`APP_EMAIL_SYSTEM_FROM`** con un remitente de ese dominio (ver [DEPLOY.md](DEPLOY.md) §3). Así podrás enviar a cualquier destinatario.

En SQL, `email_jobs.last_error` suele contener el mismo texto del 403.

## 1. Comprobar que el email existe y la cuenta está activa

En PostgreSQL (ajusta host, puerto y base; por defecto en local suele ser `localhost:5433`, base `appgestion`):

```sql
SELECT id, email, activo
FROM usuarios
WHERE lower(trim(email)) = lower(trim('tu@email.com'));
```

Si no hay filas, la API **no genera token ni encola correo**, pero responde igual (200). Si `activo` es `false`, tampoco se envía.

## 2. Resend, variables de entorno y logs (perfil local)

- Define **`RESEND_API_KEY`** (ver [`.env.example`](../.env.example) en la raíz del repo y [DEPLOY.md](DEPLOY.md) §2).
- Sin clave, el worker de correo no puede enviar vía Resend; revisa logs de la API: `email_dispatch_failed`, `RESEND_API_KEY`, `Worker email job`.
- Con **`spring.profiles.active=local`**, si el usuario es válido, la API escribe en log el **enlace completo** de recuperación (busca `enlace de recuperación de contraseña`), útil si el correo no llega.

## 3. Cola `email_jobs`

Tras solicitar recuperación con un usuario válido, debería aparecer un trabajo. Revisa estado y error:

```sql
SELECT id, status, attempts, last_error, created_at, processed_at
FROM email_jobs
ORDER BY created_at DESC
LIMIT 20;
```

- `PENDING` durante unos segundos es normal (worker cada ~3 s).
- `SENT` indica envío al proveedor.
- Errores repetidos o `last_error` con texto de Resend/API suelen indicar configuración o límites del proveedor (dominio verificado, sandbox, etc.; ver [DEPLOY.md](DEPLOY.md) §3).
