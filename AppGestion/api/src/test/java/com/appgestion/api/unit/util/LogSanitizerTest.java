package com.appgestion.api.unit.util;

import com.appgestion.api.service.AuditAccessService;
import com.appgestion.api.util.IpAnonymizer;
import com.appgestion.api.util.LogSanitizer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** SEC-23/25/31: enmascarado de datos personales en logs, CSV seguro e IP anonimizada. */
class LogSanitizerTest {

    @Test
    void maskEmail_conservaInicialYDominio() {
        assertThat(LogSanitizer.maskEmail("juan.perez@dominio.com")).isEqualTo("j***@dominio.com");
        assertThat(LogSanitizer.maskEmail("  a@b.co ")).isEqualTo("a***@b.co");
    }

    @Test
    void maskEmail_valoresInvalidos() {
        assertThat(LogSanitizer.maskEmail(null)).isEqualTo("***");
        assertThat(LogSanitizer.maskEmail("")).isEqualTo("***");
        assertThat(LogSanitizer.maskEmail("sinarroba")).isEqualTo("***");
        assertThat(LogSanitizer.maskEmail("@dominio.com")).isEqualTo("***");
        assertThat(LogSanitizer.maskEmail("usuario@")).isEqualTo("***");
    }

    @Test
    void maskEmail_noPermiteInyectarLineas() {
        assertThat(LogSanitizer.maskEmail("a@b.com\nERROR forged")).doesNotContain("\n");
    }

    @Test
    void clean_eliminaSaltosYAcota() {
        assertThat(LogSanitizer.clean("a\r\nb\tc")).isEqualTo("a__b_c");
        assertThat(LogSanitizer.clean("x".repeat(500))).hasSize(201);
        assertThat(LogSanitizer.clean(null)).isNull();
    }

    @Test
    void csvCell_neutralizaFormulas() {
        assertThat(AuditAccessService.csvCell("=HYPERLINK(\"http://evil\")")).startsWith("\"'=");
        assertThat(AuditAccessService.csvCell("+1+1")).isEqualTo("'+1+1");
        assertThat(AuditAccessService.csvCell("-2")).isEqualTo("'-2");
        assertThat(AuditAccessService.csvCell("@SUM(A1)")).isEqualTo("'@SUM(A1)");
        assertThat(AuditAccessService.csvCell("\tcmd")).isEqualTo("'\tcmd");
    }

    @Test
    void csvCell_escapaComasComillasYSaltos() {
        assertThat(AuditAccessService.csvCell(null)).isEmpty();
        assertThat(AuditAccessService.csvCell("a,b")).isEqualTo("\"a,b\"");
        assertThat(AuditAccessService.csvCell("di \"hola\"")).isEqualTo("\"di \"\"hola\"\"\"");
        assertThat(AuditAccessService.csvCell("l1\nl2")).isEqualTo("l1 l2");
        assertThat(AuditAccessService.csvCell(42)).isEqualTo("42");
    }

    @Test
    void ipAnonymizer_ipv4Ipv6() {
        assertThat(IpAnonymizer.anonymize("203.0.113.57")).isEqualTo("203.0.113.0");
        assertThat(IpAnonymizer.anonymize("2001:db8:85a3:1:2:3:4:5")).isEqualTo("2001:db8:85a3:1:0:0:0:0");
        assertThat(IpAnonymizer.anonymize(" ")).isNull();
    }
}
