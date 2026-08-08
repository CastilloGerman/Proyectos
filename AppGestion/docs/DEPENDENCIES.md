# Dependencias del proyecto

**Entorno recomendado:** JDK **21** (compilación y ejecución de la API), Node **24.14.1 LTS** (`>=24.14.1`) para el frontend, servidor **PostgreSQL 17+** (compatible con el driver JDBC gestionado por Spring Boot).

Versiones de referencia: [`pom.xml`](../pom.xml) (parent BOM), [`api/pom.xml`](../api/pom.xml), [`frontend/package.json`](../frontend/package.json). Comandos de test: [README § Tests](../README.md#-tests).

## API (Maven)

Parent BOM: **Spring Boot 4.0.6** (`spring-boot-starter-parent` en el `pom.xml` raíz). Se usa `spring-boot-starter-classic` como puente de classpath; los starters concretos heredan la versión del BOM:

| Grupo | Artefacto | Versión | Uso |
|-------|-----------|---------|-----|
| org.springframework.boot | spring-boot-starter-webmvc | 4.0.6 (BOM) | REST API |
| org.springframework.boot | spring-boot-starter-data-jpa | 4.0.6 (BOM) | Persistencia |
| org.springframework.boot | spring-boot-starter-validation | 4.0.6 (BOM) | Validación |
| org.springframework.boot | spring-boot-starter-security | 4.0.6 (BOM) | Seguridad |
| org.springframework.boot | spring-boot-starter-cache | 4.0.6 (BOM) | Abstracción `@Cacheable` |
| com.github.ben-manes.caffeine | caffeine | (BOM) | Caché in-memory (`/materiales/top-usados`) |
| org.springframework.boot | spring-boot-starter-actuator | 4.0.6 (BOM) | Health/actuator |
| org.springframework.boot | spring-boot-starter-mail | 4.0.6 (BOM) | Envío de correos |
| org.springframework.boot | spring-boot-starter-flyway | 4.0.6 (BOM) | Migraciones de esquema |
| org.flywaydb | flyway-database-postgresql | (BOM) | Soporte Flyway para PostgreSQL 17+ |
| io.jsonwebtoken | jjwt-api / jjwt-impl / jjwt-jackson | 0.12.6 | JWT |
| com.stripe | stripe-java | 25.6.0 | Pagos |
| com.bucket4j | bucket4j-core | 8.10.1 | Rate limit (auth) |
| com.github.librepdf | openpdf | 1.3.32 | Generación PDF |
| org.postgresql | postgresql | (runtime) | Base de datos |
| org.projectlombok | lombok | (optional) | Reducción de boilerplate |

### API — tests (scope `test`)

| Grupo | Artefacto | Versión | Uso |
|-------|-----------|---------|-----|
| org.springframework.boot | spring-boot-starter-test-classic | 4.0.6 (BOM) | JUnit 5, Mockito, AssertJ |
| org.springframework.security | spring-security-test | (BOM) | MockMvc + seguridad |
| com.tngtech.archunit | archunit-junit5 | 1.4.1 | Reglas de capas (`ArchitectureTest`) |
| org.testcontainers | junit-jupiter / postgresql | 1.21.2 (BOM) | Integración con PostgreSQL real |
| com.h2database | h2 | (BOM) | Perfil `test` en memoria |

## Frontend (npm)

| Paquete | Versión | Uso |
|---------|---------|-----|
| @angular/core | ^21.2 | Framework |
| @angular/material | ^21.2 | Componentes UI |
| @angular/router | ^21.2 | Navegación |
| @ngx-translate/core | ^17.0 | i18n |
| typescript | ^6.0 | Compilación (strict) |
| vitest | ^4.1 | Unit tests (`ng test` → `@angular/build:unit-test`) |
| jsdom | ^29.0 | DOM en tests |
| qrcode | ^1.5 | QR TOTP (CommonJS; declarado en `angular.json` → `allowedCommonJsDependencies`) |
| rxjs | ~7.8 | Reactividad |
