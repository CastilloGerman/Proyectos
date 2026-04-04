# Campos NULLABLE intencionales — política de auditoría BD

**Instrucción global:** en cualquier informe de auditoría de base de datos, estos campos deben aparecer con la etiqueta **`[NULLABLE INTENCIONAL - no modificar]`** y **no** deben incluirse en sugerencias de mejora (NOT NULL, migraciones coercitivas, etc.).

---

## TABLA: `audit_access_event`

| Columna     | Etiqueta                          | Razón |
|------------|-----------------------------------|--------|
| `usuario_id` | [NULLABLE INTENCIONAL - no modificar] | Los eventos pueden registrarse antes de autenticación (p. ej. intentos de login fallidos). |

---

## TABLA: `clientes`

| Columna          | Etiqueta                          | Razón |
|-----------------|-----------------------------------|--------|
| `fecha_creacion` | [NULLABLE INTENCIONAL - no modificar] | Clientes importados o migrados pueden no tener fecha de creación conocida. |
| `dni`            | [NULLABLE INTENCIONAL - no modificar] | Cliente provisional: el autónomo puede crear solo con nombre. |
| `direccion`      | [NULLABLE INTENCIONAL - no modificar] | Ídem; `estado_cliente` y validación en `FacturaService`. |
| `codigo_postal`  | [NULLABLE INTENCIONAL - no modificar] | Ídem. |
| `telefono`       | [NULLABLE INTENCIONAL - no modificar] | Ídem. |
| `email`          | [NULLABLE INTENCIONAL - no modificar] | Ídem. |
| `pais`           | [NULLABLE INTENCIONAL - no modificar] | Ídem. |
| `provincia`      | [NULLABLE INTENCIONAL - no modificar] | Ídem. |

La validación para facturar no se fuerza en BD; se hace en servicio.

---

## TABLA: `facturas`

| Columna          | Etiqueta                          | Razón |
|-----------------|-----------------------------------|--------|
| `fecha_creacion` | [NULLABLE INTENCIONAL - no modificar] | Misma línea que clientes (importación / migración). |

---

## TABLA: `materiales`

| Columna          | Etiqueta                          | Razón |
|-----------------|-----------------------------------|--------|
| `fecha_creacion` | [NULLABLE INTENCIONAL - no modificar] | Materiales importados pueden no tener fecha conocida. |

---

## TABLA: `presupuestos`

| Columna          | Etiqueta                          | Razón |
|-----------------|-----------------------------------|--------|
| `fecha_creacion` | [NULLABLE INTENCIONAL - no modificar] | Misma línea que clientes y facturas. |
| `estado`         | [NULLABLE INTENCIONAL - no modificar] | Presupuesto en borrador / en proceso sin estado asignado. |

---

## Nota sobre `base_imponible`

No existe columna `base_imponible` en el esquema AppGestion. Si una query de auditoría la incluye en un filtro, **ignorar**; no crear la columna. El cálculo corresponde a la capa de servicio.

---

## Script automatizado

El fichero [`scripts/db-audit-part2.sql`](../scripts/db-audit-part2.sql) aplica esta política en la **sección 4** mediante la columna `auditoria_nota`.
