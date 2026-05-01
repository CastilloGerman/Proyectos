package com.appgestion.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class RailwayJdbcUrlEnvironmentListenerTest {

    @Test
    void separatesCredentialsFromJdbcPostgresUrl() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty(
                        "SPRING_DATASOURCE_URL",
                        "jdbc:postgresql://railway_user:secret@mainline.proxy.rlwy.net:12345/railway?sslmode=require");

        RailwayJdbcUrlEnvironmentListener.applyRailwayPostgresUrl(environment);

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://mainline.proxy.rlwy.net:12345/railway?sslmode=require");
        assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("railway_user");
        assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("secret");
    }

    @Test
    void convertsRailwayDatabaseUrlToJdbcProperties() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty(
                        "DATABASE_URL",
                        "postgresql://railway_user:secret@postgres.railway.internal:5432/railway");

        RailwayJdbcUrlEnvironmentListener.applyRailwayPostgresUrl(environment);

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://postgres.railway.internal:5432/railway");
        assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("railway_user");
        assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("secret");
    }
}
