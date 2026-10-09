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
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import com.appgestion.api.domain.entity.*;
import com.appgestion.api.repository.PresupuestoSeguimientoAvisoRepository;
import com.appgestion.api.testsupport.TestDataBuilder;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@Transactional
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

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PresupuestoSeguimientoAvisoRepository avisoRepository;

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
        assertEquals(1, jdbc.queryForObject("""
                select count(*) from information_schema.columns
                where table_name = 'presupuestos' and column_name = 'seguimiento_silenciado'
                  and data_type = 'boolean' and column_default like 'false%'
                """, Integer.class));
        assertEquals(4, jdbc.queryForObject("""
                select count(*) from information_schema.columns
                where table_name = 'empresas' and column_name in (
                    'seguimiento_activo', 'seguimiento_dias_espera',
                    'seguimiento_max_avisos', 'seguimiento_email_resumen')
                """, Integer.class));
        assertEquals(1, jdbc.queryForObject("""
                select count(*) from pg_constraint c
                join pg_class t on t.oid = c.conrelid
                where t.relname = 'empresas'
                  and c.conname = 'ck_empresas_seguimiento_rangos' and c.contype = 'c'
                """, Integer.class));
        assertEquals(1, jdbc.queryForObject("""
                select count(*) from pg_constraint c
                join pg_class t on t.oid = c.conrelid
                where t.relname = 'presupuesto_seguimiento_aviso'
                  and c.conname = 'uk_seguimiento_aviso_presupuesto_numero' and c.contype = 'u'
                """, Integer.class));
        assertEquals(1, jdbc.queryForObject("""
                select count(*) from pg_constraint c
                join pg_class t on t.oid = c.conrelid
                where t.relname = 'presupuesto_seguimiento_aviso'
                  and c.contype = 'f' and c.confdeltype = 'c'
                """, Integer.class));
        assertEquals("42", jdbc.queryForObject("""
                select version from flyway_schema_history
                where success = true order by installed_rank desc limit 1
                """, String.class));
    }

    @Test
    void postgresInsertIgnoreIsIdempotentForDuplicateKey() {
        Organization organization = TestDataBuilder.organizationTest();
        entityManager.persist(organization);
        Usuario owner = TestDataBuilder.usuarioTest(organization);
        entityManager.persist(owner);
        Cliente client = TestDataBuilder.clienteTest(owner);
        entityManager.persist(client);
        Presupuesto budget = TestDataBuilder.presupuestoTest(owner, client);
        entityManager.persist(budget);
        entityManager.flush();

        Instant now = Instant.parse("2026-10-09T12:00:00Z");
        assertEquals(1, avisoRepository.insertIfAbsent(budget.getId(), "NO_ABIERTO", 1, now));
        assertEquals(0, avisoRepository.insertIfAbsent(budget.getId(), "NO_ABIERTO", 1, now));
    }
}
