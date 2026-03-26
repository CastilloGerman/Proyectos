# Dependencias del proyecto

**Entorno recomendado:** JDK **21** (compilación y ejecución de la API), Node **24.14.1 LTS** (`>=24.14.1`) para el frontend, servidor **PostgreSQL 17+** (compatible con el driver JDBC gestionado por Spring Boot).

## API (Maven)

| Grupo | Artefacto | Versión | Uso |
|-------|-----------|---------|-----|
| org.springframework.boot | spring-boot-starter-web | 3.5.12 | REST API |
| org.springframework.boot | spring-boot-starter-data-jpa | 3.5.12 | Persistencia |
| org.springframework.boot | spring-boot-starter-validation | 3.5.12 | Validación |
| org.springframework.boot | spring-boot-starter-security | 3.5.12 | Seguridad |
| org.springframework.boot | spring-boot-starter-actuator | 3.5.12 | Health/actuator |
| org.springframework.boot | spring-boot-starter-mail | 3.5.12 | Envío de correos |
| io.jsonwebtoken | jjwt-api / jjwt-impl / jjwt-jackson | 0.12.3 | JWT |
| com.stripe | stripe-java | 25.6.0 | Pagos |
| com.github.librepdf | openpdf | 1.3.32 | Generación PDF |
| org.postgresql | postgresql | (runtime) | Base de datos |
| org.projectlombok | lombok | (optional) | Reducción de boilerplate |

## Frontend (npm)

| Paquete | Versión | Uso |
|---------|---------|-----|
| @angular/core | ^20.3 | Framework |
| @angular/material | ^20.2 | Componentes UI |
| @angular/router | ^20.3 | Navegación |
| rxjs | ~7.8 | Reactividad |
