package com.appgestion.api.integration.multitenancy;

import com.appgestion.api.domain.entity.*;
import com.appgestion.api.domain.enums.EstadoCliente;
import com.appgestion.api.domain.enums.TipoFactura;
import com.appgestion.api.repository.ClienteRepository;
import com.appgestion.api.repository.FacturaRepository;
import com.appgestion.api.repository.OrganizationRepository;
import com.appgestion.api.repository.PresupuestoRepository;
import com.appgestion.api.repository.UsuarioRepository;

import java.time.LocalDate;

/**
 * Semilla de datos para tests de multitenancy: dos usuarios, datos de B accesibles solo por B.
 * La decisión 404 frente a 403 y el detalle DELETE/405/500 están documentados en comentarios
 * {@code //} al inicio de esta clase y en {@link MultitenancyFacturasTest#deleteFactura_noMapeado_devuelve405o500()}.
 */
public final class MultitenancyIntegrationTestSupport {

    // DECISIÓN DE SEGURIDAD:
    // Los endpoints devuelven 404 (no 403) cuando un usuario
    // intenta acceder a recursos de otro usuario.
    // Esto es intencional: no revelar si el recurso existe.
    // Referencia: OWASP - Avoid Exposing Resource Existence
    //
    // DELETE /facturas/{id} → 405 o 500: no hay ruta de borrado; la API usa anulación lógica (Parte 5,
    // POST /facturas/{id}/anular). Aceptar ambos códigos en el test es correcto.

    public static final String EMAIL_USUARIO_A = "multitenancy-block1-a@test.local";
    public static final String EMAIL_USUARIO_B = "multitenancy-block1-b@test.local";

    private MultitenancyIntegrationTestSupport() {
    }

    /**
     * @param numeroFacturaB número único por usuario; se usa para aserciones de no filtrado
     */
    public record Scenario(
            Usuario usuarioA,
            Usuario usuarioB,
            Long clienteIdA,
            Long clienteIdB,
            Long facturaIdB,
            String numeroFacturaB,
            Long presupuestoIdB
    ) {
    }

    public static Scenario seed(
            OrganizationRepository organizationRepository,
            UsuarioRepository usuarioRepository,
            ClienteRepository clienteRepository,
            FacturaRepository facturaRepository,
            PresupuestoRepository presupuestoRepository
    ) {
        Organization org = new Organization();
        org.setName("Org multitenancy block1");
        org = organizationRepository.save(org);

        Usuario usuarioA = new Usuario();
        usuarioA.setNombre("Usuario A");
        usuarioA.setEmail(EMAIL_USUARIO_A);
        usuarioA.setPasswordHash("$2a$10$dummyhashfordummytestsxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        usuarioA.setRol("USER");
        usuarioA.setActivo(true);
        usuarioA.setOrganization(org);
        usuarioA = usuarioRepository.save(usuarioA);

        Usuario usuarioB = new Usuario();
        usuarioB.setNombre("Usuario B");
        usuarioB.setEmail(EMAIL_USUARIO_B);
        usuarioB.setPasswordHash("$2a$10$dummyhashfordummytestsxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        usuarioB.setRol("USER");
        usuarioB.setActivo(true);
        usuarioB.setOrganization(org);
        usuarioB = usuarioRepository.save(usuarioB);

        Cliente clienteA = new Cliente();
        clienteA.setUsuario(usuarioA);
        clienteA.setNombre("Cliente de A");
        clienteA.setEstadoCliente(EstadoCliente.COMPLETO);
        clienteA.setDni("00000000A");
        clienteA.setDireccion("Calle A 1");
        clienteA.setCodigoPostal("28001");
        clienteA = clienteRepository.save(clienteA);

        Cliente clienteB = new Cliente();
        clienteB.setUsuario(usuarioB);
        clienteB.setNombre("Cliente de B");
        clienteB.setEstadoCliente(EstadoCliente.COMPLETO);
        clienteB.setDni("00000000B");
        clienteB.setDireccion("Calle B 1");
        clienteB.setCodigoPostal("28002");
        clienteB = clienteRepository.save(clienteB);

        LocalDate hoy = LocalDate.now();
        String numeroFacturaB = "FAC-BLOCK1-B-001";

        Factura facturaB = new Factura();
        facturaB.setUsuario(usuarioB);
        facturaB.setCliente(clienteB);
        facturaB.setNumeroFactura(numeroFacturaB);
        facturaB.setAnioFactura(hoy.getYear());
        facturaB.setNumeroSecuencial(1);
        facturaB.setFechaExpedicion(hoy);
        facturaB.setTipoFactura(TipoFactura.NORMAL);
        facturaB.setSubtotal(100.0);
        facturaB.setIva(21.0);
        facturaB.setTotal(121.0);
        facturaB.setAnulada(false);

        FacturaItem item = new FacturaItem();
        item.setFactura(facturaB);
        item.setTareaManual("Línea test");
        item.setCantidad(1.0);
        item.setPrecioUnitario(100.0);
        item.setSubtotal(100.0);
        item.setEsTareaManual(true);
        facturaB.getItems().add(item);

        facturaB = facturaRepository.save(facturaB);

        Presupuesto presupuestoB = new Presupuesto();
        presupuestoB.setUsuario(usuarioB);
        presupuestoB.setCliente(clienteB);
        presupuestoB.setEstado("Pendiente");
        presupuestoB.setSubtotal(50.0);
        presupuestoB.setIva(10.5);
        presupuestoB.setTotal(60.5);

        PresupuestoItem pItem = new PresupuestoItem();
        pItem.setPresupuesto(presupuestoB);
        pItem.setTareaManual("Ítem presupuesto B");
        pItem.setCantidad(1.0);
        pItem.setPrecioUnitario(50.0);
        pItem.setSubtotal(50.0);
        pItem.setEsTareaManual(true);
        presupuestoB.getItems().add(pItem);

        presupuestoB = presupuestoRepository.save(presupuestoB);

        return new Scenario(
                usuarioA,
                usuarioB,
                clienteA.getId(),
                clienteB.getId(),
                facturaB.getId(),
                numeroFacturaB,
                presupuestoB.getId()
        );
    }
}
