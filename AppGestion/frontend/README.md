# AppGestion Frontend

Interfaz web **Angular 21** para el SaaS de gestión de presupuestos y facturas.

## Requisitos

- Node.js **24.14.1** LTS (`>=24.14.1`; ver `.nvmrc` en la raíz del monorepo)
- npm (incluido con Node; suele ser 11.x con Node 24)

## Instalación

```bash
npm install
```

## Desarrollo

```bash
npm start
```

Equivale a `ng serve --host 0.0.0.0 --port 4200` usando el Angular CLI de `node_modules` (no hace falta instalar `ng` global).

Si prefieres invocar el CLI a mano, usa **`npx ng`** (por ejemplo `npx ng serve`, `npx ng build`). En PowerShell/CMD, el comando suelto **`ng`** solo funciona si instalaste `@angular/cli` con `npm install -g @angular/cli`.

Abre `http://localhost:4200`. El proxy (`proxy.conf.js`) envía las peticiones a `/api` al backend en `http://localhost:8081`.

## Build

```bash
npm run build
```

Salida en `dist/appgestion-frontend/`.

## Tests

```bash
npm test
```

Runner: **Vitest** vía `ng test` (builder `@angular/build:unit-test` en `angular.json`).

Cobertura principal:

| Área | Ubicación | Qué valida |
|------|-----------|------------|
| Auth | `src/app/core/auth/*.spec.ts` | Servicio, guards, interceptor, JWT `sid` |
| Cálculos negocio | `src/app/core/utils/*.spec.ts` | Totales/IVA presupuesto, importe pendiente factura |
| Servicios HTTP | `src/app/core/services/*.spec.ts` | Factura, presupuesto, cliente, etc. |
| i18n | `src/app/core/i18n/*.spec.ts` | Paridad de claves entre locales |

Convención: `TestBed` + `HttpClientTestingModule` + imports desde `vitest`.

## Estructura relevante

```
src/app/
├── core/
│   ├── auth/           # JWT, guards, interceptor
│   ├── services/       # Clientes HTTP (factura, presupuesto, …)
│   └── utils/            # Lógica pura (presupuesto-costes, factura-cobro)
├── features/             # Pantallas por dominio
└── shared/               # Componentes reutilizables
```

## Dependencias principales

| Paquete | Versión | Uso |
|---------|---------|-----|
| @angular/core | ^21.2 | Framework |
| @angular/material | ^21.2 | Componentes UI |
| @angular/router | ^21.2 | Navegación |
| @ngx-translate/core | ^17.0 | Traducciones |
| typescript | ^6.0 | Compilación |
| vitest | ^4.1 | Tests unitarios |
| rxjs | ~7.8 | Reactividad |

Detalle completo: [`docs/DEPENDENCIES.md`](../docs/DEPENDENCIES.md).
