package com.appgestion.api.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Ejecuta la normalización de {@code DATABASE_URL} / {@code SPRING_DATASOURCE_URL} en la fase
 * más temprana del arranque (antes que los listeners de aplicación), para que Flyway y el
 * DataSource vean siempre una URL {@code jdbc:postgresql://…}.
 */
public class RailwayDatasourceEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        RailwayJdbcUrlEnvironmentListener.applyRailwayPostgresUrl(environment);
    }
}
