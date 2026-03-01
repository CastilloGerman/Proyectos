# AppGestion Frontend

Interfaz web Angular 17 para el SaaS de gestión de presupuestos y facturas.

## Requisitos

- Node.js 18+
- npm 9+

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
| @angular/core | 17.3 | Framework |
| @angular/material | 17.3 | Componentes UI |
| @angular/router | 17.3 | Navegación |
| rxjs | 7.8 | Reactividad |
