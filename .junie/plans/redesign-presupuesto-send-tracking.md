---
sessionId: session-261011-153104-t8zc
---

# Plan: Rediseñar Enviar Presupuesto y Seguimiento

## Overview
Rediseñar las pantallas "Enviar presupuesto" y "Seguimiento" siguiendo el estilo de Presupuestos: botones Material, tarjetas con CSS variables, chips de estado, layout responsivo, cero colores hardcodeados.

## Implementation Notes
- `common.close` ya existe en i18n (5 idiomas) — reutilizar para snackbar
- uk.json carece de la sección `est` completa — hay que añadirla
- No hay `registerLocaleData` en el proyecto — hay que añadirlo para fechas DD/MM
- `presupuesto-rapido` depende del trigger "Compartir" — añadir `@Input()` para controlar el comportamiento
- El único hex permitido es `#25D366` (WhatsApp icon)
- No redeclarar `border-radius` en botones
- No usar MatChipsModule ni MatAlert

---

### ✓ Step 1: Tokens CSS en styles.scss
Añadir `--app-success`, `--app-success-bg`, `--app-primary` en `:root` y `html.app-dark-theme`.

### ✓ Step 2: EstadoBadgeComponent — añadir enviado, visto, caducado
Añadir 3 estados nuevos con estilos y traducciones. Hex aceptables por convención.

### ✓ Step 3: Traducciones i18n — 5 idiomas
Añadir `est.lbl.budSent/Viewed/Expired`, `est.tip.*`, `hintBanner.ariaLabel`, `hintBanner.titleDefault`.
Añadir la sección `est` completa en uk.json si falta. Reutilizar `common.close`.

### ✓ Step 4: HintBannerComponent — colapsable con --app-primary
Cabecera como `<button>`, chevron, localStorage 'expanded'/'collapsed', eliminar morados y :host-context.

### ✓ Step 5: EnviarPresupuestoComponent — Material Design
Reemplazar botones custom por Material, quitar "Compartir" y "Crear enlace público", añadir @Input() para presupuesto-rapido, MatSnackBar, mat-form-field con mat-error.

### ✓ Step 6: PresupuestoSeguimientoComponent — Material Design
Grid de datos, chips de estado, MatDialog para Revocar/Regenerar, MatSnackBar, fechas con locale.

### ✓ Step 7: PresupuestoForm — layout sin anidación
Sacar componentes del mat-card, grid de 2 columnas, check con --app-success, verificar presupuesto-rapido.

### ✓ Step 8: Registro de locales Angular
Añadir `registerLocaleData` para es, en, fr, ro, uk para que las fechas usen DD/MM.

### ✓ Step 9: Verificación final
Compilar, probar botones stroked en modo oscuro, capturas en 375/768/1280px.
