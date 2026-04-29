package com.appgestion.api.unit.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class V35MigrationSafetyTest {

    @Test
    void migrationMustNotDeleteOrReassignDuplicateClientes() throws IOException {
        String sql = new String(
                Objects.requireNonNull(getClass().getResourceAsStream(
                        "/db/migration/V35__clientes_dni_unico_por_usuario.sql")).readAllBytes(),
                StandardCharsets.UTF_8);
        String normalized = sql.toLowerCase();

        assertThat(normalized).doesNotContain("delete from public.clientes");
        assertThat(normalized).doesNotContain("set cliente_id");
        assertThat(normalized).contains("raise exception");
        assertThat(normalized).contains("uq_clientes_usuario_dni");
    }
}
