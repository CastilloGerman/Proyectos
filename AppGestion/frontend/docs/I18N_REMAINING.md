# Internacionalización: cobertura y pendientes

## Hecho recientemente (referencia rápida)

- **Dashboard** y **barra de búsqueda** (`search.*`, `dashboard.*`): textos enlazados a `ngx-translate`.
- **`EstadoBadgeComponent`**: etiquetas y tooltips vía **`est.lbl.*`** / **`est.tip.*`** (valores de API españoles se mantienen; solo cambia lo mostrado).
- **Lista de facturas** (`invList.*`, `invType.*`, `cal.m01`–`m12`): filtros, columnas y acciones.
- **`cal.m01`–`m12`**: meses para filtros/emisión (reutilizar en otros listados si hace falta).
- **Lista presupuestos** (`budList.*`), **lista clientes** (`cliList.*`), **panel cliente** (`cliPanel.*`), **formulario factura** (`factForm.*`, `payMethod.*`), **formulario presupuesto** (`budgetForm.*`; reutiliza `factForm.*` y tooltips de cliente para eliminar filas).
- **`common.cancel` / `save` / `create` / `back`**: botones estándar y navegación «atrás» (`common.back`).
- **`cliForm.*`**, **`budQuick.*`** (incl. WhatsApp `budQuick.waMsg` con interpolación), **`acctCfg.*`**, **`pwdChange.*`**, **`support.*`**: formulario cliente rápido/edición, presupuesto rápido, configuración de cuenta, cambio de contraseña, contactar soporte (`reportPresetSubject` para `?motivo=reporte`).
- **Preferencias** (`pref.*` ampliado): título de página, modo oscuro, zona horaria, moneda y estados de carga/error.

## Pantallas / archivos que suelen tener texto en castellano fijo

Sustituir gradualmente por claves `| translate` en `assets/i18n/{es,en,fr,ro,uk}.json`:

| Área | Archivos típicos |
|------|------------------|
| Presupuestos | Lista/formulario cubiertos (`budList.*`, `budgetForm.*`). **Rápido** (`budQuick.*`) hecho. |
| Clientes | Lista/panel/formulario cubiertos (`cliList.*`, `cliPanel.*`, `cliForm.*`). |
| Materiales | `material-list.component.ts`, `material-form.component.ts` |
| Facturas | Lista y formulario de factura cubiertos (`invList.*`, `factForm.*`). |
| Fiscal | `declaraciones-hacienda.component.html` |
| Cuenta (HTML) | **Hecho:** configuración cuenta, preferencias, contraseña, soporte, sesiones, historial, centro de ayuda, perfil, empresa, fiscales, suscripción, **`totp-2fa`** (`acctTotp.*`), **`metodos-pago`** (`acctPay.*`), **`plantillas-documentos`** (`acctTpl.*`; mock de ejemplo y texto largo siguen en castellano en `document-template.models.ts`). Rubros condiciones presupuesto según idioma backend si aplica. |
| Auth | **`forgot-password.component.ts`**, **`reset-password.component.ts`** |
| Diálogos compartidos | `config-empresa-dialog`, `importar-presupuesto-dialog`, `enviar-email-dialog`, `anular-factura-dialog`, etc. |

## Patrones recomendados

1. **Importar `TranslateModule`** en el standalone `imports` y usar **`'clave' | translate`** en plantillas (o `[matTooltip]="'clave' \| translate"`).
2. **Valores enviados al API** (estados `"No Pagada"`, `"Pendiente"`, etc.): mantenerlos; solo traduce **presentación** (badge ya lo hace; en `<mat-option value="No Pagada">` el `value` sigue igual, el texto puede ser `est.lbl.payUnpaid`).
3. **Meses**: reutilizar **`cal.m01`–`cal.m12`** como en factura-list.
4. **Cambiar idioma + Material**: si una etiqueta no repinta, tras `TranslateService.use` lanzar **`ChangeDetectorRef.markForCheck()` / `detectChanges()`** en el componente (ver registro anterior en `register.component.ts`).

## Idiomas

Todos los ficheros nuevos deben existir **en paralelo** en `es.json`, `en.json`, `fr.json`, `ro.json` y `uk.json` (precarga conjunta en `LanguageService.init`).
