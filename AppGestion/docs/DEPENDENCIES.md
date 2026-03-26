# Dependencias del proyecto

**Entorno recomendado:** JDK **21** (compilación y ejecución de la API), Node **24.14.1 LTS** (`>=24.14.1`) para el frontend, servidor **PostgreSQL 17+** (compatible con el driver JDBC gestionado por Spring Boot).

## API (Maven)

Parent BOM: **Spring Boot 4.0.5** (`spring-boot-starter-parent`). Se usa `spring-boot-starter-classic` como puente de classpath; los starters concretos incluyen:

| Grupo | Artefacto | Versión | Uso |
|-------|-----------|---------|-----|
| org.springframework.boot | spring-boot-starter-webmvc | 4.0.5 | REST API |
| org.springframework.boot | spring-boot-starter-data-jpa | 4.0.5 | Persistencia |
| org.springframework.boot | spring-boot-starter-validation | 4.0.5 | Validación |
| org.springframework.boot | spring-boot-starter-security | 4.0.5 | Seguridad |
| org.springframework.boot | spring-boot-starter-actuator | 4.0.5 | Health/actuator |
| org.springframework.boot | spring-boot-starter-mail | 4.0.5 | Envío de correos |
| org.springframework.boot | spring-boot-starter-flyway | 4.0.5 | Migraciones de esquema |
| org.flywaydb | flyway-database-postgresql | (BOM) | Soporte Flyway para PostgreSQL 17+ |
| io.jsonwebtoken | jjwt-api / jjwt-impl / jjwt-jackson | 0.12.6 | JWT |
| com.stripe | stripe-java | 25.6.0 | Pagos |
| com.bucket4j | bucket4j-core | 8.10.1 | Rate limit (auth) |
| com.github.librepdf | openpdf | 1.3.32 | Generación PDF |
| org.postgresql | postgresql | (runtime) | Base de datos |
| org.projectlombok | lombok | (optional) | Reducción de boilerplate |

## Frontend (npm)

| Paquete | Versión | Uso |
|---------|---------|-----|
| @angular/core | ^21.2 | Framework |
| @angular/material | ^21.2 | Componentes UI |
| @angular/router | ^21.2 | Navegación |
| typescript | ^6.0 | Compilación (strict) |
| qrcode | ^1.5 | QR TOTP (CommonJS; declarado en `angular.json` → `allowedCommonJsDependencies`) |
| rxjs | ~7.8 | Reactividad |
