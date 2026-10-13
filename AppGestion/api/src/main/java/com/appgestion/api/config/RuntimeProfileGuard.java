package com.appgestion.api.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SEC-20: evita que la API arranque en un hosting en la nube con una configuración de desarrollo.
 * <ul>
 *   <li>El perfil {@code local} (JWT y claves públicas del repositorio, {@code ddl-auto=update}, CORS permisivo,
 *       sin comprobación de suscripción) se rechaza si se detecta un entorno cloud.</li>
 *   <li>En la nube es obligatorio el perfil {@code prod}; sin él se aplicaría la configuración base.</li>
 * </ul>
 * Fuera de la nube (máquina del desarrollador, tests) no impone nada.
 */
@Component
public class RuntimeProfileGuard {

    private static final Logger log = LoggerFactory.getLogger(RuntimeProfileGuard.class);

    /** Variables que inyectan las plataformas donde se despliega la API. */
    static final List<String> CLOUD_MARKERS = List.of(
            "RAILWAY_ENVIRONMENT", "RAILWAY_PROJECT_ID", "RAILWAY_SERVICE_ID",
            "VERCEL", "VERCEL_ENV", "RENDER", "FLY_APP_NAME", "DYNO", "K_SERVICE");

    private final Environment environment;

    public RuntimeProfileGuard(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validate() {
        if (!runsInCloud()) {
            return;
        }
        if (environment.acceptsProfiles(Profiles.of("local"))) {
            throw new IllegalStateException(
                    "El perfil 'local' está activo en un entorno cloud. Usa SPRING_PROFILES_ACTIVE=prod: 'local' "
                            + "incluye secretos públicos del repositorio y desactiva controles de seguridad.");
        }
        if (!environment.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException(
                    "Entorno cloud sin perfil 'prod'. Define SPRING_PROFILES_ACTIVE=prod (o 'prod,serverless'); "
                            + "la configuración base no es apta para producción.");
        }
        log.info("RuntimeProfileGuard: entorno cloud con perfil prod.");
        warnIfDatabaseWithoutTls();
    }

    /**
     * SEC-40: advierte (sin bloquear el arranque) si la BD remota se usa sin TLS obligatorio. Con el driver de
     * PostgreSQL el valor por defecto es {@code sslmode=prefer}, que degrada a texto plano ante un atacante en la ruta.
     */
    private void warnIfDatabaseWithoutTls() {
        String url = environment.getProperty("spring.datasource.url");
        if (!isRemotePostgresWithoutStrictTls(url)) {
            return;
        }
        log.warn("SEC-40: la URL JDBC de PostgreSQL apunta a un host remoto sin sslmode=require/verify-ca/verify-full. "
                + "Añade '?sslmode=require' (o verify-full) a SPRING_DATASOURCE_URL.");
    }

    public static boolean isRemotePostgresWithoutStrictTls(String url) {
        if (url == null || !url.startsWith("jdbc:postgresql:")) {
            return false;
        }
        String lower = url.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("//localhost") || lower.contains("//127.0.0.1") || lower.contains("//[::1]")) {
            return false;
        }
        return !(lower.contains("sslmode=require") || lower.contains("sslmode=verify-ca")
                || lower.contains("sslmode=verify-full"));
    }

    private boolean runsInCloud() {
        return CLOUD_MARKERS.stream().anyMatch(name -> {
            String value = environment.getProperty(name);
            return value != null && !value.isBlank();
        });
    }
}
