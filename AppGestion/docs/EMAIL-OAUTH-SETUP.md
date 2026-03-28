# OAuth de correo (Gmail / Microsoft) — configuración

La pantalla **Datos de empresa** puede conectar **Gmail** o **Microsoft / Outlook** para enviar correos en nombre de la cuenta del usuario. El servidor debe tener registrada una **aplicación OAuth** en Google y/o Microsoft y las credenciales deben estar en variables de entorno o en `application-local.yml` (este último **no se sube a Git**).

Si no configuras nada, los botones «Conectar» permanecen deshabilitados y el modo recomendado sigue siendo **Correo de la aplicación** (Resend).

---

## Variables que lee la API

| Variable | Uso |
|----------|-----|
| `GOOGLE_OAUTH_CLIENT_ID` | Client ID de Google (tipo Web) |
| `GOOGLE_OAUTH_CLIENT_SECRET` | Secreto del cliente |
| `GOOGLE_OAUTH_REDIRECT_URI` | URL de callback (debe coincidir **exactamente** con la de la consola Google). Por defecto en código: `http://localhost:8081/auth/email/oauth/google/callback` |
| `MICROSOFT_OAUTH_CLIENT_ID` | Application (client) ID en Azure |
| `MICROSOFT_OAUTH_CLIENT_SECRET` | Client secret |
| `MICROSOFT_OAUTH_REDIRECT_URI` | Callback Microsoft (por defecto: `http://localhost:8081/auth/email/oauth/microsoft/callback`) |
| `MICROSOFT_OAUTH_TENANT` | Opcional; por defecto `common` |

Equivalente en YAML bajo `app.email.google.*` y `app.email.microsoft.*` (ver `application.yml`).

Además, para **cifrar tokens** en base de datos hace falta **`APP_EMAIL_TOKEN_SECRET`** (o `app.email.token-encryption-key`); el perfil **local** del repo define un valor por defecto solo para desarrollo.

---

## Google (Gmail API)

1. [Google Cloud Console](https://console.cloud.google.com/) → crea o elige un proyecto.
2. **APIs y servicios** → **Biblioteca** → habilita **Gmail API**.
3. **Credenciales** → **Crear credenciales** → **ID de cliente de OAuth** → tipo **Aplicación web**.
4. **URI de redirección autorizados:**  
   `http://localhost:8081/auth/email/oauth/google/callback`  
   (en producción, sustituye por `https://tu-api.tudominio.com/auth/email/oauth/google/callback`).
5. En la pantalla de consentimiento OAuth, añade **usuarios de prueba** si el estado no es “En producción”.
6. Copia **ID de cliente** y **Secreto del cliente** y configúralos como variables de entorno o en `api/src/main/resources/application-local.yml`:

```yaml
app:
  email:
    token-encryption-key: "tu-clave-local-minimo-32-caracteres-para-aes"
    google:
      client-id: "xxxxx.apps.googleusercontent.com"
      client-secret: "GOCSPX-xxxxx"
      redirect-uri: "http://localhost:8081/auth/email/oauth/google/callback"
```

Reinicia la API. El alcance solicitado incluye envío de correo (`gmail.send`).

---

## Microsoft (Microsoft Graph)

1. [Azure Portal](https://portal.azure.com/) → **Microsoft Entra ID** → **Registros de aplicaciones** → **Nuevo registro**.
2. **URI de redirección** (plataforma Web):  
   `http://localhost:8081/auth/email/oauth/microsoft/callback`
3. **Certificados y secretos** → nuevo **Secreto de cliente**.
4. **Permisos de API** → **Microsoft Graph** → delegadas → **`Mail.Send`** (y consentimiento según tu tenant).
5. Configura en local:

```yaml
app:
  email:
    microsoft:
      client-id: "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
      client-secret: "xxxxxxxx"
      redirect-uri: "http://localhost:8081/auth/email/oauth/microsoft/callback"
      tenant: "common"
```

Reinicia la API.

---

## Comprobar

- Con la API en marcha, `GET /auth/email/oauth/status` (con JWT) debe devolver `"googleOAuthConfigured": true` / `"microsoftOAuthConfigured": true` cuando las credenciales están cargadas.
- En el front, los botones **Conectar** se habilitan solo si el servidor indica que está configurado el proveedor correspondiente.
