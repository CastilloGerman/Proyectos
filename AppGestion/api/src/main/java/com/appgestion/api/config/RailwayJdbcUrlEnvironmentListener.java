package com.appgestion.api.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * Convierte {@code DATABASE_URL} / {@code SPRING_DATASOURCE_URL} estilo Railway
 * ({@code postgresql://…}) a propiedades JDBC antes de que arranquen Flyway y el DataSource.
 * <p>
 * Se registra desde {@link com.appgestion.api.AppGestionApiApplication#main} como respaldo;
 * el camino preferido es {@link RailwayDatasourceEnvironmentPostProcessor} en
 * {@code META-INF/spring.factories} ({@code org.springframework.boot.EnvironmentPostProcessor}).
 */
public class RailwayJdbcUrlEnvironmentListener
        implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final String PROPERTY_SOURCE_NAME = "railwayPostgresJdbcUrl";

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        applyRailwayPostgresUrl(event.getEnvironment());
    }

    static void applyRailwayPostgresUrl(ConfigurableEnvironment environment) {
        String raw = firstNonBlank(
                environment.getProperty("SPRING_DATASOURCE_URL"),
                environment.getProperty("DATABASE_URL"));
        if (raw == null) {
            String bound = environment.getProperty("spring.datasource.url");
            if (bound != null
                    && (bound.startsWith("postgres://") || bound.startsWith("postgresql://"))) {
                raw = bound;
            }
        }
        if (raw == null) {
            applyRailwayPgVariables(environment);
            return;
        }
        raw = raw.trim();
        if (!raw.startsWith("postgres://")
                && !raw.startsWith("postgresql://")
                && !raw.startsWith("jdbc:postgresql://")) {
            return;
        }

        try {
            JdbcParts parts = toJdbcParts(raw);
            Map<String, Object> map = new HashMap<>();
            map.put("spring.datasource.url", parts.url());
            if (!parts.username().isEmpty()) {
                map.put("spring.datasource.username", parts.username());
            }
            if (!parts.password().isEmpty()) {
                map.put("spring.datasource.password", parts.password());
            }

            MutablePropertySources sources = environment.getPropertySources();
            sources.remove(PROPERTY_SOURCE_NAME);
            sources.addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, map));
        } catch (Exception e) {
            throw new IllegalStateException(
                    "No se pudo convertir DATABASE_URL / SPRING_DATASOURCE_URL a JDBC: " + e.getMessage(), e);
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }

    private static JdbcParts toJdbcParts(String rawUrl) {
        String postgresUrl = rawUrl.startsWith("jdbc:postgresql://")
                ? rawUrl.substring("jdbc:".length())
                : rawUrl;
        String forUri = postgresUrl.replaceFirst("^postgres(ql)?://", "http://");
        URI uri = URI.create(forUri);

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host vacío en URL de Postgres");
        }
        int port = uri.getPort();
        if (port < 0) {
            port = 5432;
        }
        String path = uri.getPath();
        if (path == null || path.isBlank() || "/".equals(path)) {
            throw new IllegalArgumentException("nombre de base de datos vacío en URL de Postgres");
        }
        String database = cleanDatabaseName(path.startsWith("/") ? path.substring(1) : path);

        String username = "";
        String password = "";
        String userInfo = uri.getRawUserInfo();
        if (userInfo != null && !userInfo.isBlank()) {
            int colon = userInfo.indexOf(':');
            if (colon < 0) {
                // Railway often provides PGUSER/PGPASSWORD separately. A single value before '@'
                // is usually a malformed URL fragment, so avoid treating it as the DB user.
                username = "";
            } else {
                username = decode(userInfo.substring(0, colon));
                password = decode(userInfo.substring(colon + 1));
            }
        }

        StringBuilder jdbc = new StringBuilder();
        jdbc.append("jdbc:postgresql://").append(host).append(":").append(port).append("/").append(database);
        String query = cleanQuery(uri.getRawQuery());
        if (query != null) {
            jdbc.append("?").append(query);
        }

        return new JdbcParts(jdbc.toString(), username, password);
    }

    private static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    private static void applyRailwayPgVariables(ConfigurableEnvironment environment) {
        String host = firstNonBlank(environment.getProperty("PGHOST"), environment.getProperty("POSTGRES_HOST"));
        String database = firstNonBlank(
                environment.getProperty("PGDATABASE"),
                environment.getProperty("POSTGRES_DB"));
        if (host == null || database == null) {
            return;
        }

        String port = firstNonBlank(environment.getProperty("PGPORT"), environment.getProperty("POSTGRES_PORT"));
        if (port == null) {
            port = "5432";
        }

        Map<String, Object> map = new HashMap<>();
        map.put("spring.datasource.url", "jdbc:postgresql://" + host.trim() + ":" + port.trim()
                + "/" + cleanDatabaseName(database));

        String username = firstNonBlank(environment.getProperty("PGUSER"), environment.getProperty("POSTGRES_USER"));
        String password = firstNonBlank(
                environment.getProperty("PGPASSWORD"),
                environment.getProperty("POSTGRES_PASSWORD"));
        if (username != null) {
            map.put("spring.datasource.username", username);
        }
        if (password != null) {
            map.put("spring.datasource.password", password);
        }

        MutablePropertySources sources = environment.getPropertySources();
        sources.remove(PROPERTY_SOURCE_NAME);
        sources.addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, map));
    }

    private static String cleanDatabaseName(String database) {
        String cleaned = decode(database).trim();
        int queryStart = cleaned.indexOf('?');
        if (queryStart >= 0) {
            cleaned = cleaned.substring(0, queryStart);
        }
        return cleaned.replace("\uFFFD", "").replaceAll("\\?+$", "");
    }

    private static String cleanQuery(String query) {
        if (query == null) {
            return null;
        }
        String cleaned = query.trim().replaceFirst("^\\?+", "");
        if (cleaned.isBlank() || cleaned.chars().allMatch(ch -> ch == '?')) {
            return null;
        }
        return cleaned;
    }

    private record JdbcParts(String url, String username, String password) {}
}
