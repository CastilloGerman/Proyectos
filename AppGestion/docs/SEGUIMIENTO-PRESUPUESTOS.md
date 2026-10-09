# Seguimiento automático de presupuestos

La API revisa diariamente los presupuestos enviados y crea avisos **solo para el propietario**. No envía mensajes a clientes ni crea enlaces públicos.

## Reglas

- Se consideran únicamente presupuestos enviados, no silenciados y cuyo estado comercial sea `Pendiente`. `Aceptado`, `Rechazado`, `En ejecución` (también la variante legada `En ejecucion`) y cualquier estado desconocido quedan excluidos.
- El aviso `NO_ABIERTO` se genera cuando no existe ninguna vista de un enlace público y han pasado los días configurados desde el envío.
- El aviso `ABIERTO_SIN_RESPUESTA` se genera cuando existe alguna vista y han pasado los días configurados desde la vista más reciente. Cambios de estado, vistas recientes y silenciamiento se comprueban de nuevo en cada ejecución.
- Por defecto se esperan **3 días** entre el envío o la vista más reciente y el primer aviso, y se permite un máximo de **2 avisos por presupuesto**. Los avisos posteriores están separados por al menos el mismo número de días naturales; nunca se crea más de uno por presupuesto y fecha local.
- Solo se procesa al propietario con cuenta activa y suscripción `ACTIVE`, `TRIAL_ACTIVE` o `TRIALING`. Cuentas inactivas, trials expirados y suscripciones vencidas quedan fuera.
- Cada aviso crea una notificación en la aplicación, localizada en español, inglés, francés, rumano o ucraniano según el idioma del usuario. El enlace de acción es una ruta privada de la aplicación.
- El email es opcional y apagado por defecto. Si se activa, se encola un único resumen diario por usuario con los avisos nuevos de esa ejecución. Incluye cliente, número interno de presupuesto, tipo, días transcurridos, URL de la aplicación e instrucciones para desactivar resúmenes. No incluye tokens ni URLs públicas. Un error al encolar el resumen se registra con identificador interno y cantidad, sin incluir datos del cliente.

## API y preferencias

- `GET /config/empresa/seguimiento-presupuestos`
- `PATCH /config/empresa/seguimiento-presupuestos` con campos opcionales: `seguimientoActivo`, `seguimientoDiasEspera` (1–30), `seguimientoMaxAvisos` (1–5), `seguimientoEmailResumen`.
- `POST /presupuestos/{id}/seguimiento/silenciar` detiene los avisos de ese presupuesto.
- `DELETE /presupuestos/{id}/seguimiento/silenciar` reactiva el seguimiento.

Las operaciones de presupuesto verifican la propiedad por `usuarioId`; un presupuesto ajeno se presenta como no encontrado. La respuesta de detalle incluye `seguimientoAvisosEnviados`, `seguimientoUltimoAvisoAt` y `seguimientoSilenciado`.

## Ejecución y entorno local

El job se ejecuta por defecto cada día a las **09:00 Europe/Madrid**. La hora se puede ajustar con `APP_PRESUPUESTO_SEGUIMIENTO_CRON` (expresión cron de Spring); el huso horario permanece fijado a `Europe/Madrid`.

En el perfil `local` exclusivamente está disponible `POST /dev/presupuesto-seguimiento/ejecutar`. Usa `APP_PRESUPUESTO_SEGUIMIENTO_LOCAL_DAYS_OVERRIDE` (por defecto `0`) para probar sin esperar los días configurados. El límite de un aviso por presupuesto y día sigue aplicándose. Este endpoint no se registra en otros perfiles.

Para probarlo localmente:

1. Arranca API con perfil `local`, inicia sesión y configura `seguimientoActivo=true`.
2. Envía un presupuesto pendiente y comprueba el resultado con `GET /presupuestos/{id}`.
3. Llama `POST /dev/presupuesto-seguimiento/ejecutar`; revisa las notificaciones y los avisos del detalle.
4. Llama otra vez el mismo día: no debe duplicar el aviso. Prueba también silenciar/reactivar y el resumen de email si el proveedor de correo está configurado.

La persistencia usa una clave única por presupuesto y número de aviso y bloquea cada presupuesto durante la decisión para evitar duplicados entre workers concurrentes. No existe un coordinador global del job: varias instancias pueden ejecutar el lote y consumir recursos repetidos, pero el bloqueo de fila y la restricción única protegen la creación de avisos.

No se incluye en esta fase una función de avisos por caducidad próxima.

## Validar V42 contra PostgreSQL

La prueba `PostgresFlywayValidationTest` usa Testcontainers y se omite si Docker no está disponible. Para validar manualmente sin Docker, crea una base de datos PostgreSQL temporal vacía y arranca la API contra ella con Flyway habilitado y Hibernate en modo `validate`:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'local'
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5432/appgestion_seguimiento_tmp'
$env:SPRING_DATASOURCE_USERNAME = 'postgres'
$env:SPRING_DATASOURCE_PASSWORD = '<contraseña-local>'
$env:SPRING_JPA_HIBERNATE_DDL_AUTO = 'validate'
$env:JWT_SECRET = 'local-validation-secret-at-least-32-characters'
.\mvnw.cmd -pl api spring-boot:run
```

La comprobación termina correctamente si Flyway aplica V42 y el contexto arranca sin errores de validación de esquema. Interrumpe la API con Ctrl+C y elimina únicamente la base temporal creada para esta prueba; no apuntes a una base con datos que quieras conservar.
