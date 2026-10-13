package com.appgestion.api.unit.fiscal;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MigracionTipoIvaScriptTest {

    @Test
    void migracion_soloRellenaTipoIva() throws Exception {
        String lower = sinComentarios(Files.readString(Path.of("src/main/resources/db/migration/V46__factura_items_tipo_iva.sql")));

        assertThat(lower).contains("add column tipo_iva");
        assertThat(lower).doesNotContain("update facturas");
        assertThat(lower).doesNotContain("cuota_iva");
        assertThat(lower).doesNotContain("set subtotal");
        assertThat(lower).doesNotContain("set iva");
        assertThat(lower).doesNotContain("set total");
    }

    @Test
    void rollback_abortaSiHayTiposDistintosDe21O0() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/rollback/V46__factura_items_tipo_iva_rollback.sql"));

        assertThat(sql).contains("RAISE EXCEPTION");
        assertThat(sql).contains("IS DISTINCT FROM 21");
        assertThat(sql).contains("IS DISTINCT FROM 0");
        assertThat(sql.toLowerCase()).contains("antes de crear facturas");
        assertThat(sql.toLowerCase()).contains("10 por ciento");
        assertThat(sql.toLowerCase()).contains("4 por ciento");
    }

    @Test
    void precheck_usaElEsquemaAnteriorATipoIva() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/verification/V46_tipo_iva_precheck.sql"));
        String lower = sql.toLowerCase();

        assertThat(lower).contains("antes de v46");
        assertThat(lower).contains("aplica_iva");
        assertThat(lower).contains("0.01 * count");
        assertThat(lower).doesNotContain("factura_items.tipo_iva");
        assertThat(lower).doesNotContain("i.tipo_iva");
    }

    private static String sinComentarios(String sql) {
        return sql.replaceAll("(?m)--.*$", "").toLowerCase();
    }
}
