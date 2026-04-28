package com.appgestion.api.unit.migration;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FacturaMontoCobradoMigrationTest {

    @Test
    void v2CreatesFacturasWhenDatabaseWasBaselinedWithoutTable() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:v2_factura_baseline;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                "");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS public");
            statement.execute("""
                    CREATE TABLE public.usuarios (
                        id BIGSERIAL PRIMARY KEY
                    )
                    """);
            statement.execute("""
                    CREATE TABLE public.clientes (
                        id BIGSERIAL PRIMARY KEY
                    )
                    """);
            statement.execute("""
                    CREATE TABLE public.presupuestos (
                        id BIGSERIAL PRIMARY KEY
                    )
                    """);

            String migration = readMigration();

            assertDoesNotThrow(() -> statement.execute(migration));

            assertTrue(columnExists(connection, "FACTURAS", "MONTO_COBRADO"));
        }
    }

    private static String readMigration() throws Exception {
        try (InputStream input = FacturaMontoCobradoMigrationTest.class.getResourceAsStream(
                "/db/migration/V2__factura_monto_cobrado.sql")) {
            if (input == null) {
                throw new IllegalStateException("No se encontró V2__factura_monto_cobrado.sql");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static boolean columnExists(Connection connection, String table, String column) throws Exception {
        try (var columns = connection.getMetaData().getColumns(null, "PUBLIC", table, column)) {
            return columns.next();
        }
    }
}
