# AppGestion

SaaS multiusuario para gestión de presupuestos y facturas. Aplicación web con Angular, API REST en Spring Boot y base de datos PostgreSQL.

**Documentación:** [Despliegue en producción](docs/DEPLOY.md) · [Modelo organización / tenant](docs/TENANT-MODEL.md)

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
│   Spring Boot 3.5 (API REST)         │
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

## Orden de activación

Sigue este orden para arrancar la aplicación (cada paso depende del anterior):

```
1. PostgreSQL (base de datos)
       ↓
2. API (Spring Boot)
       ↓
3. Frontend (Angular)
       ↓
4. [Opcional] Stripe CLI (solo si usas webhooks)
```

---

## Inicio rápido

### 1. Base de datos (PostgreSQL)

**PostgreSQL debe estar en ejecución antes que la API.**

Crear base de datos y usuario (ajusta puerto si usas 5433):

```sql
CREATE DATABASE appgestion;
CREATE USER appgestion WITH PASSWORD 'tu_password';
GRANT ALL PRIVILEGES ON DATABASE appgestion TO appgestion;
```

Por defecto la API usa `localhost:5433`. Si tu PostgreSQL está en 5432, configura:
```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/appgestion"
```

### 2. API (Spring Boot)

El `pom.xml` del módulo `api` activa el perfil **`local`** en `spring-boot:run` (JWT por defecto solo desarrollo, `skip-check` de suscripción, `ddl-auto: update`).

```powershell
cd api
mvn clean compile spring-boot:run
```

O con el script: `.\api\iniciar-api.ps1`

Si arrancas la API **sin** perfil `local` (p. ej. desde el IDE), define al menos `JWT_SECRET` (≥32 caracteres) o activa `--spring.profiles.active=local`.

**Variables opcionales** (PowerShell):
```powershell
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "postgres"
$env:JWT_SECRET = "tu-clave-secreta-minimo-32-caracteres"
# Para suscripciones Stripe:
$env:STRIPE_SECRET_KEY = "sk_test_..."
$env:STRIPE_PRICE_MONTHLY = "price_..."
# Orígenes CORS (producción): lista separada por comas
$env:CORS_ALLOWED_ORIGINS = "https://tu-dominio.com"
```

**Producción:** `SPRING_PROFILES_ACTIVE=prod`, `JWT_SECRET` fuerte, `CORS_ALLOWED_ORIGINS`, claves Stripe reales (el arranque en `prod` falla si detecta placeholders). Ver también `docs/TENANT-MODEL.md`.

La API estará en `http://localhost:8081`. Espera a ver "Started AppGestionApiApplication" antes de continuar.

### 3. Frontend (Angular)

**La API debe estar corriendo antes de usar el frontend.**

```powershell
cd frontend
npm install
npm start
```

La aplicación estará en `http://localhost:4200`. El proxy redirige `/api` a la API en 8081.

### 4. [Opcional] Webhooks Stripe

Solo si quieres que el estado de suscripción se actualice automáticamente tras pagar/cancelar:

```bash
stripe listen --forward-to localhost:8081/api/webhook/stripe
```

Copia el `whsec_...` y configura `STRIPE_WEBHOOK_SECRET`.

## Estructura del proyecto

```
AppGestion/
├── api/                    # Backend Spring Boot
│   ├── src/main/java/
│   │   └── com/appgestion/api/
│   │       ├── config/     # Security, Stripe, migration
│   │       ├── constant/   # Constantes fiscales (IVA)
│   │       ├── controller/
│   │       ├── domain/
│   │       │   ├── entity/
│   │       │   └── enums/
│   │       ├── dto/
│   │       ├── repository/
│   │       ├── scheduler/  # Jobs programados
│   │       ├── security/  # JWT, filters
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

| Variable | Descripción | Default / notas |
|----------|-------------|------------------|
| `SPRING_PROFILES_ACTIVE` | `local` (dev) / `prod` (despliegue) | Sin `local`, exige `JWT_SECRET` y perfil seguro |
| `SPRING_DATASOURCE_URL` | JDBC URL PostgreSQL | Ver `application.yml` |
| `JWT_SECRET` | Clave HS256 (≥32 caracteres fuera de `local`) | Obligatorio si no usas perfil `local` |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos (coma) | En `prod` acotar explícitamente |
| `STRIPE_SECRET_KEY` | Clave API Stripe | Vacío por defecto; `local` trae placeholder de desarrollo |
| `STRIPE_WEBHOOK_SECRET` | Secreto webhook Stripe | Validado en `prod` |
| `STRIPE_PRICE_MONTHLY` | ID del precio mensual | Configurar para checkout |

### Stripe (suscripciones)

Para que el botón "Activar suscripción" funcione, debes configurar credenciales reales de Stripe. Los valores por defecto (`sk_test_xxx`, `price_xxx`) son placeholders y provocan error 400.

#### 1. Obtener la Secret Key

1. Entra en [Stripe Dashboard](https://dashboard.stripe.com) (crea cuenta si no tienes).
2. Ve a **Developers → API keys**.
3. Copia la **Secret key** (empieza por `sk_test_` en modo test).

#### 2. Crear producto y precio

1. En Stripe: **Products → Add product**.
2. Crea un producto (ej. "Suscripción mensual AppGestion").
3. Añade un precio recurrente (mensual).
4. Copia el **Price ID** (empieza por `price_`).

#### 3. Configurar variables de entorno

**PowerShell (Windows):**
```powershell
$env:STRIPE_SECRET_KEY = "sk_test_TU_CLAVE_REAL_AQUI"
$env:STRIPE_PRICE_MONTHLY = "price_TU_ID_REAL"
```

**Bash (Linux/Mac):**
```bash
export STRIPE_SECRET_KEY="sk_test_TU_CLAVE_REAL_AQUI"
export STRIPE_PRICE_MONTHLY="price_TU_ID_REAL"
```

Reinicia la API después de definir las variables.

#### 4. Webhooks (opcional, para actualizar estado de suscripción)

Para que el estado de suscripción se actualice automáticamente tras pagar o cancelar:

```bash
stripe listen --forward-to localhost:8081/api/webhook/stripe
```

Copia el `whsec_...` que muestra el comando y configúralo:

```powershell
$env:STRIPE_WEBHOOK_SECRET = "whsec_..."
```

## Funcionalidades

- **Autenticación**: Registro, login, JWT, guard de rutas
- **Multiusuario**: Cada usuario ve solo sus clientes, presupuestos y facturas
- **Suscripciones**: Checkout Stripe, webhook, bloqueo sin suscripción activa
- **CRUD**: Clientes, presupuestos (con líneas), facturas (con líneas)
- **Customer Portal**: Gestión de facturación vía Stripe

## Dependencias

- **API**: Spring Boot 3.5, JWT (jjwt 0.12), Stripe Java, PostgreSQL. Ver `docs/DEPENDENCIES.md`.
- **Frontend**: Angular 17, Angular Material, RxJS. Ver `frontend/README.md`.

## Licencia

Proyecto privado.
