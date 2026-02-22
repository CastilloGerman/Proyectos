# Diagnóstico del error 403 Forbidden

## Objetivo
Identificar la causa exacta del 403 sin modificar más código de seguridad.

---

## Paso 1: Verificar qué muestra la API al arrancar

Al iniciar la API con `.\api\iniciar-api.ps1`, busca en la consola:

```
SubscriptionCheckFilter: skip-check=true (true=omitir suscripción, false=exigir suscripción activa)
```

- **Si aparece `skip-check=true`**: La comprobación de suscripción está desactivada. El 403 no viene del SubscriptionCheckFilter.
- **Si aparece `skip-check=false`**: El SubscriptionCheckFilter está bloqueando. Revisa `application.yml` y perfil activo.

---

## Paso 2: Ver qué se registra cuando ocurre el 403

Cuando hagas una petición que devuelva 403 (por ejemplo, GET /api/presupuestos), mira la consola de la API:

| Mensaje en logs | Origen | Causa |
|-----------------|--------|-------|
| `403 Forbidden: usuario X sin suscripción activa - GET /api/presupuestos` | SubscriptionCheckFilter | Usuario sin suscripción y skip-check=false |
| `403 Forbidden: usuario X sin permisos para GET /api/presupuestos` | AccessDeniedHandler (Spring Security) | La ruta no coincide con permitAll o el usuario no cumple hasRole |
| `403 Forbidden: usuario anonymous sin permisos...` | AccessDeniedHandler | El JWT no se envía, está mal o expirado → usuario anónimo |

---

## Paso 3: Probar la API directamente (sin proxy)

Abre una terminal y ejecuta:

```powershell
# 1. Obtener un token (reemplaza email y password con tus credenciales)
$body = @{ email = "tu@email.com"; password = "tu_password" } | ConvertTo-Json
$login = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" -Method POST -Body $body -ContentType "application/json"
$token = $login.token

# 2. Probar GET presupuestos CON token
Invoke-RestMethod -Uri "http://localhost:8081/api/presupuestos" -Headers @{ Authorization = "Bearer $token" }

# 3. Probar GET presupuestos SIN token (debería fallar si requiere auth)
Invoke-RestMethod -Uri "http://localhost:8081/api/presupuestos"
```

Interpretación:
- Si (2) funciona y (3) falla: El backend exige autenticación. El frontend podría no estar enviando el token.
- Si (2) y (3) fallan: El backend está bloqueando incluso con token válido.
- Si (2) funciona: El problema puede estar en el proxy o en cómo el frontend envía el token.

---

## Paso 4: Comprobar el token en el frontend

En el navegador (F12 → Application → Local Storage), busca la clave donde se guarda el token (por ejemplo `appgestion_token`). Verifica que existe y no está vacía después de hacer login.

---

## Paso 5: Comprobar la petición en Network

En F12 → Network, haz una petición que falle con 403 y revisa:

1. **Request URL**: ¿Es `http://localhost:4200/api/presupuestos`? (correcto, el proxy reenvía a 8081)
2. **Request Headers**: ¿Hay `Authorization: Bearer <token>`?
3. **Response**: ¿Qué cuerpo devuelve? (ej. `{"error":"Suscripción requerida"}` vs `{"error":"Acceso denegado. No tienes permisos..."}`)

---

## Resumen de posibles causas

| Causa | Cómo comprobarlo |
|-------|------------------|
| skip-check=false en runtime | Log al arrancar |
| Token no enviado o expirado | Network tab + Local Storage |
| Path no coincide (context-path /api) | Logs muestran "anonymous" o ruta distinta |
| Proxy no reenvía correctamente | Probar con curl/Invoke-RestMethod directo a 8081 |

---

## Siguiente paso

Después de ejecutar los pasos 1–5, anota:
1. Valor de `skip-check` al arrancar
2. Mensaje exacto en logs cuando ocurre el 403
3. Si la petición directa a 8081 con token funciona o no
4. Si la petición desde el navegador incluye el header `Authorization`

Con eso se puede acotar la causa y aplicar un cambio mínimo y dirigido.
