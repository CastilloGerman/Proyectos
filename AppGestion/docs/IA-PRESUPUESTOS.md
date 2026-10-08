# Borradores de presupuesto con IA

`POST /presupuestos/ia/borrador` genera un borrador editable y no persiste presupuestos. La IA nunca decide importes: solo transcribe en `precioDictado` una cifra que el profesional haya dicho explícitamente, y el backend acepta esa cifra únicamente si coincide con un número normalizado del texto original. Un dictado válido prevalece sobre el catálogo; `precioCatalogo` conserva el importe del material como referencia y `precioOrigen` indica `dictado`, `catalogo` o `ninguno`. Los importes ausentes, inválidos o no verificables se descartan; si tampoco hay precio de catálogo positivo, `faltaPrecio=true`.

`precioTipo` distingue `unitario` de `total`. Un total con cantidad 1 o sin cantidad se representa como una unidad; con cantidad superior a 1 se divide entre la cantidad con dos decimales y redondeo HALF_UP, y la confianza se reduce para impedir su confirmación rápida automática. La pantalla conserva todas las partidas separadas: si el profesional dicta un único importe para varios elementos (por ejemplo, dos rollos de cable), el modelo debe asociarlo una sola vez como total a la primera partida y marcar en las restantes `precioIncluidoEnLineaAnterior`. La interfaz muestra «Precio incluido en la línea anterior» además de mantener el indicador de precio pendiente. No se distribuye ni duplica ese importe; el profesional debe revisar esa asignación.

Los números se normalizan para admitir `1200`, `1.200`, `1 200`, `12,5` y `12.5` (en español, punto o espacio de millares y coma decimal). Además de coincidir literalmente, una cifra solo se acepta como precio si tiene contexto de importe: moneda cercana (`euros`, `€`, `pavos`, `pelas`, `eurillos`, `lereles`) o una expresión de precio como «a 45 el bote», «cada uno a 25», «cobrar 350», «por 1200 cerrados» o «sale por 40». Cantidades, medidas y dimensiones sin ese contexto (por ejemplo, `60 metros cuadrados`, `120 por 70`, `15 litros`, `Simon 82` o `2,5`) no se aceptan como precios. Los importes escritos solo con palabras, como «mil doscientos», tampoco se reconocen: deben dictarse con cifras. La palabra «unos», «más o menos» o «calcula» puede marcar `precioAproximado`; una partida aproximada sigue bloqueando la confirmación rápida y requiere revisión explícita.

Las cantidades globales y la mano de obra/trabajo cerrado se expresan como 1 y no son dudosas. «Un/una/otro» más un sustantivo contado implica cantidad 1. Una partida contable sin cantidad deducible permanece vacía y se marca `cantidadDudosa`; nunca se muestra una cantidad predeterminada de 1 como si hubiera sido dictada. Los borradores con más de 50 filas o filas no estructurables se rechazan en vez de descartar partidas en silencio. En ese caso, la interfaz muestra un mensaje localizado indicando que hay demasiadas partidas o datos incompletos y recomienda dividir la descripción y comprobar que cada partida tenga descripción.

La cuota específica se configura con `APP_AI_PRESUPUESTO_REQUESTS_PER_DAY` (por defecto 20). El límite horario común de borradores Gemini sigue configurándose con `APP_AI_GEMINI_REQUESTS_PER_HOUR` (por defecto 20). Además, `APP_AI_GEMINI_MAX_ATTEMPTS_PER_HOUR` limita a 60 por defecto el total de intentos enviados al proveedor por usuario y hora; cuenta llamadas correctas, fallidas y cada reintento interno, separado de las cuotas de borradores válidos. Los tres contadores usan Caffeine en memoria local: no coordinan límites entre instancias y se reinician al reiniciar el proceso.

La evaluación manual con Gemini real está desactivada por defecto. En PowerShell, con `GEMINI_API_KEY` disponible en el entorno:

```powershell
$env:APP_AI_EVAL_GEMINI = 'true'
$env:APP_AI_GEMINI_ENABLED = 'true'
.\mvnw.cmd -pl api "-Dtest=PresupuestoIaGeminiEvaluationTest" test
```

El test utiliza 20 descripciones no personales de `api/src/test/resources/ia-presupuesto-casos.json`, incluidas cinco regresiones con importes dictados y total esperado; no imprime la clave ni el texto original.

El test imprime el total calculado frente al esperado para las cinco regresiones y un resumen de precios dictados correctos, precios inventados (el objetivo es cero), precios dictados omitidos, partidas omitidas y partidas fusionadas, además de métricas de cantidades/materiales. Los casos incluyen distractores de dimensiones, acabado y tipo de producto, trabajos sin material de catálogo, precios aproximados, mano de obra y componentes distintos que no deben fusionarse. La evaluación usa anotaciones aproximadas y requiere revisión humana.

La omisión observada en el ejemplo de gotelé se atribuye principalmente a que el prompt anterior pedía extraer partidas pero no exigía conservar todos los materiales mencionados. El normalizador también tenía dos descartes silenciosos (más de 50 filas y descripciones vacías), aunque ninguno explica una omisión ordinaria de tres materiales; ya se instruye la extracción completa y ahora se rechazan explícitamente esas respuestas incompletas/malformadas. Como no se registra ni conserva la respuesta del proveedor por privacidad, no es posible probar retrospectivamente cuál de esas causas afectó a una respuesta concreta.

En «Confirmar las seguras», los precios dictados explícitos sí pueden incluirse y el diálogo muestra el importe y la etiqueta «Precio dictado». Se excluyen los aproximados, importes repartidos entre varias unidades (la confianza se baja), partidas con precio/cantidad pendientes y sugerencias de confianza baja. La confirmación de cada línea sigue siendo obligatoria antes de guardar.

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

## Comprobación determinista de medidas

Después de validar que el material sugerido pertenece al usuario y al conjunto de candidatos, el backend compara las medidas reconocibles de la descripción de cada partida con las del nombre del material. Si la descripción incluye medidas pero no hay coincidencia con el material (o el nombre del material no contiene medidas reconocibles), se elimina la asociación y el precio queda en cero con `faltaPrecio=true`. Si la descripción no contiene una medida reconocible, esta comprobación no altera la asociación.

La heurística reconoce pares dimensionales como `120x70`, `120 x 70` y `120 por 70` (sin distinguir mayúsculas ni el signo `×`); diámetro con `Ø40`/`40 mm`; longitudes explícitas en milímetros o centímetros; metros decimales como `1,2 m`/`1.2 m`; y cantidades de elementos como `12 elementos`. Compara pares sin depender del orden de sus lados y normaliza longitudes métricas a milímetros, de modo que `1,2 m`, `120 cm` y `1200 mm` se consideran equivalentes para esta comparación. La conversión se limita a las unidades y contextos reconocidos por los patrones; no representa una conversión general de unidades ni maneja tolerancias. Las cantidades con unidades de superficie como `6 m2` no se tratan como dimensión de producto.

La comparación usa la descripción de partida estructurada (`tareaManual`) y el nombre del candidato, no intenta interpretar toda la frase de la obra contra cada línea: así evita comparar, por ejemplo, los metros cuadrados del suelo con la dimensión del plato de ducha. Si la descripción contiene medidas reconocibles y el nombre del material no contiene ninguna medida reconocible, se elimina la asociación y se marca `faltaPrecio=true`. Si la descripción no contiene medidas reconocibles, la comprobación no cambia la asociación. Es una defensa determinista y conservadora, no un analizador semántico completo. No reconoce medidas escritas solo con palabras salvo que el modelo las convierta a cifras en la descripción; no comprende equivalencias de producto, unidades imperiales, tolerancias ni dimensiones implícitas, y puede no detectar una incompatibilidad si la descripción omite la medida o el nombre del catálogo no la expresa en un formato reconocido. También puede retirar una asociación si una cifra de la descripción que parezca medida no sea una especificación del producto. El profesional debe revisar material, cantidad y precio en el borrador.

La etiqueta de confianza (`alta`, `media`, `baja`) la propone el modelo y no es una garantía de que el material o las medidas sean correctos. No debe usarse como validación del producto ni como autorización para omitir la revisión visible; la acción de confirmación rápida aplica además las reglas deterministas de seguridad de la interfaz y presenta antes una lista para aceptación explícita.

Antes de producción, revisa los términos vigentes del plan de la API de Gemini (gratuito o de pago), las condiciones de tratamiento y retención de datos del proveedor y la política de privacidad de AppGestion. No presupongas que el plan gratuito y el de pago tienen las mismas condiciones.
