package com.appgestion.api.testsupport;

import com.appgestion.api.domain.entity.Cliente;
import com.appgestion.api.domain.entity.Factura;
import com.appgestion.api.domain.entity.Organization;
import com.appgestion.api.domain.entity.Presupuesto;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.EstadoCliente;
import com.appgestion.api.domain.enums.TipoFactura;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidades mínimas válidas para tests unitarios o de integración (persistir vía {@code EntityManager}).
 */
public final class TestDataBuilder {

    private TestDataBuilder() {
    }

    public static Organization organizationTest() {
        Organization o = new Organization();
        o.setName("Organización test");
        return o;
    }

    public static Usuario usuarioTest() {
        return usuarioTest(organizationTest());
    }

    public static Usuario usuarioTest(Organization organization) {
        Usuario u = new Usuario();
        u.setNombre("Usuario test");
        u.setEmail("test-" + UUID.randomUUID() + "@example.com");
        u.setPasswordHash("$2a$10$dummyhashfordummytestsxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        u.setOrganization(organization);
        return u;
    }

    public static Cliente clienteTest(Usuario usuario) {
        Cliente c = new Cliente();
        c.setUsuario(usuario);
        c.setNombre("Cliente test");
        c.setEstadoCliente(EstadoCliente.COMPLETO);
        return c;
    }

    public static Presupuesto presupuestoTest(Usuario usuario, Cliente cliente) {
        Presupuesto p = new Presupuesto();
        p.setUsuario(usuario);
        p.setCliente(cliente);
        p.setEstado("Pendiente");
        p.setSubtotal(100.0);
        p.setIva(21.0);
        p.setTotal(121.0);
        p.setIvaHabilitado(true);
        return p;
    }

    public static Factura facturaTest(Usuario usuario, Cliente cliente) {
        Factura f = new Factura();
        f.setUsuario(usuario);
        f.setCliente(cliente);
        f.setNumeroFactura("F-TEST-001");
        f.setAnioFactura(LocalDate.now().getYear());
        f.setNumeroSecuencial(1);
        f.setFechaExpedicion(LocalDate.now());
        f.setTipoFactura(TipoFactura.NORMAL);
        return f;
    }
}
