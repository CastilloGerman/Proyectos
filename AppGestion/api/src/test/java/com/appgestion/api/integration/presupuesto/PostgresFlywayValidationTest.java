package com.appgestion.api.integration.presupuesto;

import com.appgestion.api.AppGestionApiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PostgresFlywayValidationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        properties.add("spring.flyway.enabled", () -> true);
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void flywaySchemaMatchesPostgresEntityTypesAndConstraints() {
        assertEquals("character varying", jdbc.queryForObject("""
                select data_type from information_schema.columns
                where table_name = 'presupuesto_enlace' and column_name = 'token_hash'
                """, String.class));
        assertEquals(64, jdbc.queryForObject("""
                select character_maximum_length from information_schema.columns
                where table_name = 'presupuesto_enlace' and column_name = 'token_hash'
                """, Integer.class));
        assertEquals("timestamp with time zone", jdbc.queryForObject("""
                select data_type from information_schema.columns
                where table_name = 'presupuesto_enlace' and column_name = 'creado_at'
                """, String.class));
        assertEquals(1, jdbc.queryForObject("""
                select count(*) from pg_constraint c
                join pg_class t on t.oid = c.conrelid
                where t.relname = 'presupuesto_enlace' and c.contype = 'f' and c.confdeltype = 'c'
                """, Integer.class));
        assertEquals(1, jdbc.queryForObject("""
                select count(*) from pg_indexes
                where tablename = 'presupuesto_enlace' and indexdef ilike '%unique%token_hash%'
                """, Integer.class));
    }
}
