# AppGestion

SaaS multiusuario para gestión de presupuestos y facturas. Aplicación web con Angular, API REST en Spring Boot y base de datos PostgreSQL.

## Arquitectura

```
┌─────────────────────────────────────┐
│   Angular 17 (Frontend SPA)         │
│   - Login/Registro JWT              │
│   - CRUD Clientes, Presupuestos,    │
│     Facturas                        │
│   - Suscripciones Stripe            │
└─────────────────┬───────────────────┘
                  │ HTTP/REST
┌─────────────────▼───────────────────┐
│   Spring Boot 3.2 (API REST)         │
│   - Autenticación JWT                │
│   - Seguridad por usuario            │
│   - Integración Stripe               │
└─────────────────┬───────────────────┘
                  │
┌─────────────────▼───────────────────┐
│   PostgreSQL                        │
└─────────────────────────────────────┘
```

## Requisitos

- **Java 17**
- **Maven 3.9+**
- **Node.js 18+**
- **PostgreSQL 14+**

## Inicio rápido

### 1. Base de datos

Crear base de datos y usuario:

```sql
CREATE DATABASE appgestion;
CREATE USER appgestion WITH PASSWORD 'tu_password';
GRANT ALL PRIVILEGES ON DATABASE appgestion TO appgestion;
```

### 2. API

```bash
# Configurar variables de entorno (opcional)
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/appgestion
export SPRING_DATASOURCE_USERNAME=appgestion
export SPRING_DATASOURCE_PASSWORD=tu_password
export JWT_SECRET=tu-clave-secreta-minimo-32-caracteres

# Compilar y ejecutar
mvn clean compile
cd api && mvn spring-boot:run
```

La API estará en `http://localhost:8080/api`.

### 3. Frontend

```bash
cd frontend
npm install
npm start
```

La aplicación estará en `http://localhost:4200`. El proxy redirige `/api` a la API.

## Estructura del proyecto

```
AppGestion/
├── api/                    # Backend Spring Boot
│   ├── src/main/java/
│   │   └── com/appgestion/api/
│   │       ├── config/     # Security, Stripe
│   │       ├── controller/
│   │       ├── domain/entity/
│   │       ├── dto/
│   │       ├── repository/
│   │       ├── security/   # JWT, guards
│   │       └── service/
│   └── src/main/resources/
│       └── application.yml
├── frontend/               # Angular SPA
│   └── src/app/
│       ├── core/           # Auth, services, models
│       ├── features/       # Módulos por funcionalidad
│       └── shared/
├── pom.xml                 # Parent Maven
└── README.md
```

## Configuración

### API (application.yml)

| Variable | Descripción | Default |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | JDBC URL PostgreSQL | `jdbc:postgresql://localhost:5432/appgestion` |
| `JWT_SECRET` | Clave para firmar tokens | (requerido en producción) |
| `STRIPE_SECRET_KEY` | Clave API Stripe | `sk_test_xxx` |
| `STRIPE_WEBHOOK_SECRET` | Secreto webhook Stripe | `whsec_xxx` |

### Stripe (suscripciones)

1. Crear producto y precio en [Stripe Dashboard](https://dashboard.stripe.com).
2. Configurar `STRIPE_PRICE_MONTHLY` con el ID del precio.
3. Para webhooks locales: `stripe listen --forward-to localhost:8080/api/webhook/stripe`

## Funcionalidades

- **Autenticación**: Registro, login, JWT, guard de rutas
- **Multiusuario**: Cada usuario ve solo sus clientes, presupuestos y facturas
- **Suscripciones**: Checkout Stripe, webhook, bloqueo sin suscripción activa
- **CRUD**: Clientes, presupuestos (con líneas), facturas (con líneas)
- **Customer Portal**: Gestión de facturación vía Stripe

## Dependencias

- **API**: Spring Boot 3.2, JWT (jjwt 0.12), Stripe Java, PostgreSQL. Ver `docs/DEPENDENCIES.md`.
- **Frontend**: Angular 17, Angular Material, RxJS. Ver `frontend/README.md`.

## Licencia

Proyecto privado.
