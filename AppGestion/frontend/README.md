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
