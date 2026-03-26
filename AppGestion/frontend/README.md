# AppGestion Frontend

Interfaz web Angular 20 para el SaaS de gestión de presupuestos y facturas.

## Requisitos

- Node.js 24.14.1 LTS (ver `.nvmrc` / `.node-version` en la raíz del monorepo)
- npm (incluido con Node; suele ser 11.x con Node 24)

## Instalación

```bash
npm install
```

## Desarrollo

```bash
npm start
```

Equivale a `ng serve` usando el Angular CLI instalado en `node_modules` (no hace falta instalar `ng` global).

Si prefieres invocar el CLI a mano, usa **`npx ng`** (por ejemplo `npx ng serve`, `npx ng build`). En PowerShell/CMD, el comando suelto **`ng`** solo funciona si instalaste `@angular/cli` con `npm install -g @angular/cli`.

Abre `http://localhost:4200`. El proxy envía las peticiones a `/api` al backend en `http://localhost:8081`.

## Build

```bash
npm run build
```

Salida en `dist/appgestion-frontend/`.

## Dependencias principales

| Paquete | Versión | Uso |
|---------|---------|-----|
| @angular/core | ^20.3 | Framework |
| @angular/material | ^20.2 | Componentes UI |
| @angular/router | ^20.3 | Navegación |
| rxjs | 7.8 | Reactividad |
