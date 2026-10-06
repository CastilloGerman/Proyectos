# Borradores de presupuesto con IA

`POST /presupuestos/ia/borrador` genera un borrador editable y no persiste presupuestos. Los precios y las unidades asociados a un material se leen del catálogo del usuario; los materiales sin precio o con precio cero se devuelven con `faltaPrecio=true`.

La cuota específica se configura con `APP_AI_PRESUPUESTO_REQUESTS_PER_DAY` (por defecto 20). El límite horario común de borradores Gemini sigue configurándose con `APP_AI_GEMINI_REQUESTS_PER_HOUR` (por defecto 20). Además, `APP_AI_GEMINI_MAX_ATTEMPTS_PER_HOUR` limita a 60 por defecto el total de intentos enviados al proveedor por usuario y hora; cuenta llamadas correctas, fallidas y cada reintento interno, separado de las cuotas de borradores válidos. Los tres contadores usan Caffeine en memoria local: no coordinan límites entre instancias y se reinician al reiniciar el proceso.

La evaluación manual con Gemini real está desactivada por defecto. En PowerShell, con `GEMINI_API_KEY` disponible en el entorno:

```powershell
$env:APP_AI_EVAL_GEMINI = 'true'
$env:APP_AI_GEMINI_ENABLED = 'true'
.\mvnw.cmd -pl api "-Dtest=PresupuestoIaGeminiEvaluationTest" test
```

El test utiliza 15 descripciones no personales de `api/src/test/resources/ia-presupuesto-casos.json`; no imprime la clave ni el texto original.

El test imprime el modelo seleccionado y un resumen separado de precios inventados, cantidades inventadas, falsos positivos de material, aciertos al dejar sin material las partidas sin coincidencia de catálogo, partidas esperadas omitidas y porcentaje aproximado de partidas correctas. Los 15 casos incluyen distractores de dimensiones, acabado y tipo de producto, además de trabajos sin material correspondiente en catálogo. La evaluación usa anotaciones aproximadas y requiere revisión humana.

Para comparar modelos, ejecuta el mismo test dos veces con IDs compatibles distintos. El modelo solo se selecciona con `APP_AI_GEMINI_MODEL`; la clave no se pasa en argumentos ni se imprime:

```powershell
$env:APP_AI_GEMINI_MODEL = 'gemini-3.5-flash-lite'
.\mvnw.cmd -pl api "-Dtest=PresupuestoIaGeminiEvaluationTest" test

$env:APP_AI_GEMINI_MODEL = '<ID-del-segundo-modelo-compatible>'
.\mvnw.cmd -pl api "-Dtest=PresupuestoIaGeminiEvaluationTest" test
```

Sustituye el marcador por un ID de modelo habilitado para la clave y cuenta de Google usadas. Compara las líneas `resumen` de cada ejecución.

## Privacidad y proveedores

En una llamada a Gemini se envían el texto que el profesional escribe o dicta para el borrador y una lista acotada de candidatos del catálogo del usuario (ID, nombre y unidad). No se envían el teléfono, email, DNI ni dirección del cliente leídos de la base de datos. `clienteId` solo se usa en backend para comprobar que pertenece al usuario autenticado y se devuelve como referencia del borrador; no se incorpora al prompt ni se precarga información personal del cliente.

La aplicación no registra el texto, el prompt ni la respuesta completa del modelo. Registra el modelo, los contadores de tokens disponibles, la latencia y el resultado. El procesamiento del texto por el proveedor ocurre fuera de la base de datos de AppGestion.

Antes de producción, revisa los términos vigentes del plan de la API de Gemini (gratuito o de pago), las condiciones de tratamiento y retención de datos del proveedor y la política de privacidad de AppGestion. No presupongas que el plan gratuito y el de pago tienen las mismas condiciones.
