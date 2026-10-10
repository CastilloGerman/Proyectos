# Respuesta del cliente desde el enlace público (Fase 6b)

## Resumen

El cliente puede pulsar **«Me interesa»** o **«Tengo dudas»** desde la página pública del presupuesto (`/p/:token`). Esto genera un **aviso** al contratista, **no una aceptación contractual**: no cambia el estado del presupuesto, no registra firma, ni nombre ni importe aceptado. El contratista decide qué hacer (llamar, cambiar el estado él mismo).

## Flujo

1. El contratista envía un presupuesto con enlace público (Fase 6).
2. El cliente abre `/p/:token` y ve dos botones bajo el total.
3. Al pulsar uno, aparece un diálogo con un campo de mensaje opcional (máx. 500 caracteres).
4. El servidor guarda la opción, la fecha y el mensaje en las columnas `respuesta_cliente`, `respuesta_cliente_at` y `respuesta_cliente_mensaje` de `presupuestos`.
5. Se crea una **notificación in-app** localizada para el contratista con enlace al presupuesto.
6. Se envía un **email** al contratista (si tiene email) con la información.
7. El estado del presupuesto **no cambia**.

## Datos guardados

| Columna | Tipo | Descripción |
|---------|------|-------------|
| `respuesta_cliente` | `VARCHAR(10)` | `INTERESA` o `DUDAS` (nullable) |
| `respuesta_cliente_at` | `TIMESTAMPTZ` | Fecha/hora del último cambio (nullable) |
| `respuesta_cliente_mensaje` | `VARCHAR(500)` | Mensaje opcional del cliente, texto plano (nullable) |
| `permitir_respuesta_cliente` | `BOOLEAN` | Interruptor en `empresas` (default `TRUE`) |

## Por qué NO cambia el estado

- Es un **aviso**, no una aceptación. El cliente no firma ni confirma importe.
- El contratista mantiene el control total: decide si llama, si acepta, si cambia el estado.
- Evita problemas legales: no hay contrato implícito.

## Límite anti-abuso

- **Cualquier diferencia** (opción o mensaje) se trata como una actualización sujeta al límite de **5 minutos**.
- Si la opción y el mensaje son **idénticos** a lo ya enviado, no se hace nada (idempotente).
- Si la opción es la misma pero el mensaje es **distinto**, se actualiza el mensaje y se notifica una vez (sujeto al cooldown).
- Si intenta cambiar antes de 5 minutos, recibe `429 Too Many Requests` con `Retry-After: 300` y el mensaje "Espera unos minutos para modificar tu aviso".
- Peticiones concurrentes se resuelven con bloqueo pesimista (`PESSIMISTIC_WRITE`) + actualización condicional atómica; **exactamente 1 notificación** por cambio efectivo.

## Interacción con el seguimiento (Fase 7)

Un presupuesto con `respuesta_cliente` no nulo **NO genera avisos de seguimiento** mientras siga pendiente. La condición se centraliza en `PresupuestoSeguimientoService`:

```java
|| presupuesto.getRespuestaCliente() != null
```

Esto evita spam al contratista cuando ya sabe que el cliente está interesado o tiene dudas.

## Interruptor de activación

El contratista puede desactivar la función desde **Configuración → Seguimiento de presupuestos**:

- **Interruptor**: «Permitir que el cliente me avise desde el enlace» (activado por defecto).
- Si está desactivado, el GET público no muestra los botones y el POST responde `404`.

## Endpoint

```
POST /publico/presupuestos/{token}/responder
```

**Body:**
```json
{
  "opcion": "INTERESA",
  "mensaje": "¿Podéis incluir el transporte?"
}
```

**Respuestas:**
- `200 OK` — aviso enviado.
- `400 Bad Request` — opción inválida o mensaje muy largo.
- `404 Not Found` — token inexistente/caducado/revocado o función desactivada.
- `429 Too Many Requests` — límite de 5 minutos entre cambios.

## Validación y saneado

- `opcion` solo acepta `INTERESA` o `DUDAS`.
- `mensaje` máximo 500 caracteres; se eliminan caracteres de control (`\p{Cc}`).
- El mensaje se escapa con `EmailCopy.htmlEscape()` en el email.
- En la UI se muestra como texto plano (sin HTML).
- **Logs**: nunca se registran tokens, URLs públicas ni mensajes del cliente.

## Texto recomendado para política de privacidad

> «Cuando el cliente utiliza el enlace público para indicar su interés o enviar un mensaje, almacenamos la opción seleccionada, la fecha y el mensaje (máximo 500 caracteres). Estos datos solo son visibles para la empresa destinataria del presupuesto y se utilizan exclusivamente para facilitar la comunicación entre ambas partes.»

## Archivos modificados

### Backend

| Archivo | Cambio |
|---------|--------|
| `V43__respuesta_cliente_enlace_publico.sql` | Migración: 3 columnas en `presupuestos`, 1 en `empresas` |
| `Presupuesto.java` | Campos `respuestaCliente`, `respuestaClienteAt`, `respuestaClienteMensaje` |
| `Empresa.java` | Campo `permitirRespuestaCliente` (default `true`) |
| `RespuestaClienteRequest.java` | DTO de entrada con validación |
| `RespuestaClienteConfirmacionResponse.java` | DTO de respuesta |
| `PresupuestoRespuestaClienteService.java` | Lógica principal: resolución de token, cooldown, saneado, notificación, email |
| `PresupuestoPublicoController.java` | Endpoint `POST /{token}/responder` con rate limit y cabeceras |
| `SecurityConfig.java` | `permitAll()` para `/publico/presupuestos/*/responder` |
| `NotificacionService.java` | Método `respuestaClientePresupuesto()` con 5 idiomas |
| `EmailService.java` | Método `enviarRespuestaCliente()` |
| `PresupuestoSeguimientoService.java` | Excluye presupuestos con `respuestaCliente` de los avisos |
| `PublicBudgetLinkService.java` | `permiteResponder` y `respuestaCliente` en el GET público |
| `PresupuestoService.java` | Mapeo de respuesta en `PresupuestoResponse` |
| `PresupuestoPublicoResponse.java` | Campos `permiteResponder`, `respuestaCliente` |
| `PresupuestoResponse.java` | Campos `respuestaCliente`, `respuestaClienteAt`, `respuestaClienteMensaje` |
| `SeguimientoPresupuestosPatchRequest.java` | Campo `permitirRespuestaCliente` |
| `SeguimientoPresupuestosResponse.java` | Campo `permitirRespuestaCliente` |

### Frontend

| Archivo | Cambio |
|---------|--------|
| `presupuesto.model.ts` | Interfaces `PresupuestoPublico`, `PresupuestoRespuestaClienteRequest` |
| `presupuesto.service.ts` | Método `responderPublico()` |
| `presupuesto-publico.component.ts` | Botones, diálogo, validación, estados de envío |
| `presupuesto-seguimiento.component.ts` | Bloque de respuesta del cliente con mensaje y acciones |
| `seguimiento-presupuestos.component.ts` | Interruptor «Permitir que el cliente me avise» |
| `config.service.ts` | Mapeo `permitsRespuestaCliente` ↔ `permitirRespuestaCliente` |
| `i18n/*.json` (5 archivos) | Traducciones completas en ES, EN, FR, RO, UK |

## Verificación (PowerShell)

```powershell
# Backend
cd C:\Users\German\Documents\Proyectos\AppGestion
.\mvnw.cmd -pl api test

# Frontend
cd frontend
npm.cmd test -- --watch=false
npm.cmd run build
```

## Decisiones y límites

- **Sin tabla de evidencia**: solo columnas en `presupuestos`. Prioriza simplicidad.
- **Sin nombre del firmante**: el aviso no identifica legalmente al cliente.
- **Email activado por defecto**: el contratista recibe email y notificación in-app.
- **5 minutos de cooldown**: suficiente para evitar abuso sin frustrar al cliente.
- **Sin «Marcar como atendida»**: el contratista gestiona el estado directamente desde el presupuesto.
- **No avanza a Fase 5 ni Fase 8**: solo Fase 6b.
