package com.appgestion.api.integration.auth;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.config.migration.EncryptedSecretsMigrationRunner;
import com.appgestion.api.domain.entity.Empresa;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.repository.OrganizationRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.security.TotpService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/** SEC-06: secreto TOTP y contraseña SMTP cifrados en reposo, con compatibilidad y migración de valores heredados. */
@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class EncryptedSecretsAtRestTest {

    private static final String SMTP_PASSWORD = "p@ss-smtp-ñ-con-símbolos!";

    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private EmpresaRepository empresaRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private TotpService totpService;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private EncryptedSecretsMigrationRunner migrationRunner;

    private String rawTotp(Long id) {
        return jdbc.queryForObject("SELECT totp_secret FROM usuarios WHERE id = ?", String.class, id);
    }

    private String rawMailPassword(Long id) {
        return jdbc.queryForObject("SELECT mail_password FROM empresas WHERE id = ?", String.class, id);
    }

    private Usuario seedTotpUser() {
        return AuthLoginSeedSupport.seedTotpUser(organizationRepository, usuarioRepository, passwordEncoder, totpService);
    }

    @Test
    void secretoTotp_seGuardaCifrado_yAlLeerseDescifra() {
        Usuario u = seedTotpUser();
        String plano = u.getTotpSecret();
        entityManager.flush();

        String enBd = rawTotp(u.getId());
        assertThat(enBd).startsWith("enc:v1:").doesNotContain(plano);

        entityManager.clear();
        assertThat(usuarioRepository.findById(u.getId()).orElseThrow().getTotpSecret()).isEqualTo(plano);
    }

    @Test
    void secretoTotpHeredadoEnTextoPlano_seLee_yElRunnerLoCifra() {
        Usuario u = seedTotpUser();
        entityManager.flush();
        jdbc.update("UPDATE usuarios SET totp_secret = ? WHERE id = ?", "JBSWY3DPEHPK3PXP", u.getId());
        entityManager.clear();

        assertThat(usuarioRepository.findById(u.getId()).orElseThrow().getTotpSecret()).isEqualTo("JBSWY3DPEHPK3PXP");

        migrationRunner.encryptLegacyPlaintextSecrets();
        assertThat(rawTotp(u.getId())).startsWith("enc:v1:").doesNotContain("JBSWY3DPEHPK3PXP");

        entityManager.clear();
        assertThat(usuarioRepository.findById(u.getId()).orElseThrow().getTotpSecret()).isEqualTo("JBSWY3DPEHPK3PXP");

        // Idempotente: una segunda ejecución no vuelve a cifrar lo ya cifrado.
        String yaCifrado = rawTotp(u.getId());
        migrationRunner.encryptLegacyPlaintextSecrets();
        assertThat(rawTotp(u.getId())).isEqualTo(yaCifrado);
    }

    @Test
    void passwordSmtp_seGuardaCifrada_yAlLeerseDescifra() {
        Usuario u = seedTotpUser();
        Empresa e = new Empresa();
        e.setUsuario(u);
        e.setNombre("Empresa test");
        e.setMailUsername("ventas@example.com");
        e.setMailPassword(SMTP_PASSWORD);
        e = empresaRepository.saveAndFlush(e);

        assertThat(rawMailPassword(e.getId())).startsWith("enc:v1:").doesNotContain("p@ss");

        entityManager.clear();
        assertThat(empresaRepository.findById(e.getId()).orElseThrow().getMailPassword()).isEqualTo(SMTP_PASSWORD);
    }

    @Test
    void passwordSmtpHeredadaEnTextoPlano_seCifraConElRunner() {
        Usuario u = seedTotpUser();
        Empresa e = new Empresa();
        e.setUsuario(u);
        e.setNombre("Empresa test");
        e = empresaRepository.saveAndFlush(e);
        jdbc.update("UPDATE empresas SET mail_password = ? WHERE id = ?", "legacy-plain", e.getId());
        entityManager.clear();

        migrationRunner.encryptLegacyPlaintextSecrets();

        assertThat(rawMailPassword(e.getId())).startsWith("enc:v1:").doesNotContain("legacy-plain");
        entityManager.clear();
        assertThat(empresaRepository.findById(e.getId()).orElseThrow().getMailPassword()).isEqualTo("legacy-plain");
    }
}
