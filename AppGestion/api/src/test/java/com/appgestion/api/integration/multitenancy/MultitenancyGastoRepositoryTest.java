package com.appgestion.api.integration.multitenancy;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.domain.entity.Gasto;
import com.appgestion.api.domain.entity.Organization;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.GastoCategoria;
import com.appgestion.api.repository.GastoRepository;
import com.appgestion.api.repository.OrganizationRepository;
import com.appgestion.api.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class MultitenancyGastoRepositoryTest {

    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private GastoRepository gastoRepository;

    private Usuario usuarioA;
    private Usuario usuarioB;
    private Long gastoIdB;

    @BeforeEach
    void setUp() {
        Organization org = new Organization();
        org.setName("Org gastos multitenancy");
        org = organizationRepository.save(org);

        usuarioA = new Usuario();
        usuarioA.setNombre("Usuario A gastos");
        usuarioA.setEmail("gastos-mt-a@test.local");
        usuarioA.setPasswordHash("$2a$10$dummyhashfordummytestsxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        usuarioA.setRol("USER");
        usuarioA.setActivo(true);
        usuarioA.setOrganization(org);
        usuarioA = usuarioRepository.save(usuarioA);

        usuarioB = new Usuario();
        usuarioB.setNombre("Usuario B gastos");
        usuarioB.setEmail("gastos-mt-b@test.local");
        usuarioB.setPasswordHash("$2a$10$dummyhashfordummytestsxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        usuarioB.setRol("USER");
        usuarioB.setActivo(true);
        usuarioB.setOrganization(org);
        usuarioB = usuarioRepository.save(usuarioB);

        Gasto gastoB = new Gasto();
        gastoB.setUsuario(usuarioB);
        gastoB.setProveedor("Proveedor B");
        gastoB.setConcepto("Compra B");
        gastoB.setFecha(LocalDate.now());
        gastoB.setBaseImponible(100.0);
        gastoB.setTipoIva(21.0);
        gastoB.setCuotaIva(21.0);
        gastoB.setCategoria(GastoCategoria.MATERIAL);
        gastoB = gastoRepository.save(gastoB);
        gastoIdB = gastoB.getId();
    }

    @Test
    void findByIdAndUsuarioId_gastoDeOtroUsuario_vacio() {
        assertThat(gastoRepository.findByIdAndUsuarioId(gastoIdB, usuarioA.getId())).isEmpty();
    }

    @Test
    void findByUsuarioId_noIncluyeGastosDeOtroTenant() {
        assertThat(gastoRepository.findByUsuarioIdOrderByFechaDesc(usuarioA.getId())).isEmpty();
        assertThat(gastoRepository.findByUsuarioIdOrderByFechaDesc(usuarioB.getId()))
                .hasSize(1)
                .first()
                .extracting(Gasto::getId)
                .isEqualTo(gastoIdB);
    }

    @Test
    void existsByIdAndUsuarioId_gastoDeOtroUsuario_false() {
        assertThat(gastoRepository.existsByIdAndUsuarioId(gastoIdB, usuarioA.getId())).isFalse();
        assertThat(gastoRepository.existsByIdAndUsuarioId(gastoIdB, usuarioB.getId())).isTrue();
    }
}
