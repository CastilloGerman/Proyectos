package com.appgestion.api.integration.presupuesto;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.domain.entity.*;
import com.appgestion.api.repository.PresupuestoRepository;
import com.appgestion.api.repository.PresupuestoSeguimientoAvisoRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.testsupport.TestDataBuilder;
import com.appgestion.api.controller.LocalPresupuestoSeguimientoController;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.hibernate.exception.ConstraintViolationException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class PresupuestoSeguimientoPersistenceTest {
    @Autowired EntityManager entityManager;
    @Autowired PresupuestoSeguimientoAvisoRepository avisos;
    @Autowired PresupuestoRepository presupuestos;
    @Autowired UsuarioRepository usuarios;
    @Autowired ApplicationContext applicationContext;
    @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping;

    @Test
    void insertIfAbsentIsIdempotentAndCascadesByBudgetForeignKey() {
        Organization organization = TestDataBuilder.organizationTest();
        entityManager.persist(organization);
        Usuario owner = TestDataBuilder.usuarioTest(organization);
        entityManager.persist(owner);
        Cliente client = TestDataBuilder.clienteTest(owner);
        entityManager.persist(client);
        Presupuesto budget = TestDataBuilder.presupuestoTest(owner, client);
        budget.setEnviadoAt(LocalDateTime.of(2026, 10, 6, 10, 0));
        entityManager.persist(budget);
        entityManager.flush();

        assertThat(usuarios.findFollowupOwnerIdsAfter(0L, PageRequest.of(0, 100)).getContent())
                .containsExactly(owner.getId());
        assertThat(presupuestos
                .findByUsuarioIdAndEnviadoAtIsNotNullAndSeguimientoSilenciadoFalseAndIdGreaterThanOrderByIdAsc(
                        owner.getId(), 0L, PageRequest.of(0, 100)))
                .extracting(Presupuesto::getId).containsExactly(budget.getId());

        Instant createdAt = Instant.parse("2026-10-09T12:00:00Z");
        assertThat(avisos.insertIfAbsent(budget.getId(), "NO_ABIERTO", 1, createdAt)).isEqualTo(1);
        assertThat(avisos.insertIfAbsent(budget.getId(), "NO_ABIERTO", 1, createdAt)).isZero();
        assertThat(avisos.countByPresupuestoId(budget.getId())).isEqualTo(1);
        entityManager.createNativeQuery("DELETE FROM presupuestos WHERE id = :id")
                .setParameter("id", budget.getId()).executeUpdate();
        assertThat(avisos.countByPresupuestoId(budget.getId())).isZero();
        entityManager.flush();
        assertThat(presupuestos
                .findByUsuarioIdAndEnviadoAtIsNotNullAndSeguimientoSilenciadoFalseAndIdGreaterThanOrderByIdAsc(
                        owner.getId(), 0L, PageRequest.of(0, 100)))
                .isEmpty();
    }

    @Test
    void companyDatabaseConstraintsRejectDaysOutsideOneThroughThirty() {
        Organization organization = TestDataBuilder.organizationTest();
        entityManager.persist(organization);
        Usuario owner = TestDataBuilder.usuarioTest(organization);
        entityManager.persist(owner);
        Empresa company = new Empresa();
        company.setUsuario(owner);
        company.setNombre("Follow-up settings");
        entityManager.persist(company);
        entityManager.flush();

        assertThatThrownBy(() -> entityManager.createNativeQuery(
                "UPDATE empresas SET seguimiento_dias_espera = 0 WHERE usuario_id = :id")
                .setParameter("id", owner.getId()).executeUpdate())
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void manualJobEndpointIsAbsentOutsideLocalAndSecurityRequiresAuthentication() throws Exception {
        assertThat(applicationContext.getBeansOfType(LocalPresupuestoSeguimientoController.class)).isEmpty();
        assertThat(handlerMapping.getHandlerMethods().keySet())
                .noneMatch(mapping -> mapping.getPatternValues()
                        .contains("/dev/presupuesto-seguimiento/ejecutar"));

        String securityConfig = Files.readString(Path.of(
                "src/main/java/com/appgestion/api/config/SecurityConfig.java"));
        assertThat(securityConfig)
                .contains("\"/dev/**\"", "auth.requestMatchers(appEndpoints).authenticated()")
                .doesNotContain("\"/dev/presupuesto-seguimiento/ejecutar\"");
    }
}
