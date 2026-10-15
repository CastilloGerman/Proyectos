# Ficheros del reparto de descuento en `cad694c`

Commit `cad694c` («auditoria devsecops»). El reparto va mezclado con la auditoría.
Para quitar solo el reparto, revierte estas partes a mano. No reviertas el fichero entero
cuando también tiene cambios de SEC-15 o SEC-16.

## Ficheros enteros

Se añadieron en ese commit y son solo del reparto:

- `AppGestion/api/src/main/java/com/appgestion/api/fiscal/RepartoDescuento.java`
- `AppGestion/api/src/test/java/com/appgestion/api/unit/fiscal/RepartoDescuentoTest.java`
- `AppGestion/api/src/test/java/com/appgestion/api/integration/presupuesto/FacturarPresupuestoDescuentoTest.java`

## Partes dentro de ficheros compartidos

### `AppGestion/api/src/main/java/com/appgestion/api/service/FacturaService.java`

Del reparto:

- import de `RepartoDescuento`
- import de `ArrayList`
- `copiarItemsDesdePresupuesto` (reparte la base neta y elige el precio unitario)
- `dinero`
- `precioUnitarioDeLinea`
- el comentario de la opción B en `calcularTotales`

No son del reparto: `FacturaAuditoriaService` (SEC-15) ni `normalizarEstadoPago` (SEC-16).

### `AppGestion/api/src/main/java/com/appgestion/api/service/PresupuestoService.java`

Del reparto: los dos comentarios del cálculo en `double` (en `mapItems` y en `calcularTotales`).

No es del reparto: `normalizarEstado` (SEC-16).

### `AppGestion/frontend/src/app/core/utils/presupuesto-costes.util.ts`

Del reparto: el comentario del ticket para migrar el cálculo a decimal.

## Fuera de este commit

La edición que conserva la base neta al guardar una factura está en la rama
`fix-edicion-factura-descuento`, no en `cad694c`.
