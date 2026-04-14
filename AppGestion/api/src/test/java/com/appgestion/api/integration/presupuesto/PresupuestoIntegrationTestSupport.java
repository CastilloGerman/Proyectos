package com.appgestion.api.integration.presupuesto;

import com.appgestion.api.domain.entity.Cliente;
import com.appgestion.api.domain.entity.Empresa;
import com.appgestion.api.domain.entity.Organization;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.EstadoCliente;
import com.appgestion.api.repository.ClienteRepository;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.repository.OrganizationRepository;
import com.appgestion.api.repository.UsuarioRepository;

/**
 * Semilla Block 4: usuario con empresa fiscal mínima y un cliente COMPLETO para flujos que requieren facturación.
 * Los casos con cliente provisional usan {@code POST /clientes/provisional} en el propio test.
 */
public final class PresupuestoIntegrationTestSupport {

    public static final String EMAIL_USUARIO_PRESUPUESTOS = "presupuestos-block4@test.local";

    /** Misma convención que {@link com.appgestion.api.integration.facturacion.FacturaIntegrationTestSupport}. */
    public static final String NIF_EMPRESA = "00000000T";
    public static final String NIF_CLIENTE_COMPLETO = "00000001R";

    public record Scenario(Long usuarioId, Long clienteCompletoId) {
    }

    private PresupuestoIntegrationTestSupport() {
    }

    public static Scenario seed(
            OrganizationRepository organizationRepository,
            UsuarioRepository usuarioRepository,
            ClienteRepository clienteRepository,
            EmpresaRepository empresaRepository) {
        Organization org = new Organization();
        org.setName("Org presupuestos block4");
        org = organizationRepository.save(org);

        Usuario usuario = new Usuario();
        usuario.setNombre("Usuario presupuestos B4");
        usuario.setEmail(EMAIL_USUARIO_PRESUPUESTOS);
        usuario.setPasswordHash("$2a$10$dummyhashfordummytestsxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        usuario.setRol("USER");
        usuario.setActivo(true);
        usuario.setOrganization(org);
        usuario = usuarioRepository.save(usuario);

        Empresa empresa = new Empresa();
        empresa.setUsuario(usuario);
        empresa.setNombre("Empresa test B4");
        empresa.setNif(NIF_EMPRESA);
        empresa.setCodigoPostal("28001");
        empresa.setProvincia("Madrid");
        empresa.setPais("España");
        empresaRepository.save(empresa);

        Cliente completo = new Cliente();
        completo.setUsuario(usuario);
        completo.setNombre("Cliente completo B4");
        completo.setEstadoCliente(EstadoCliente.COMPLETO);
        completo.setDni(NIF_CLIENTE_COMPLETO);
        completo.setDireccion("Calle Presupuesto 1");
        completo.setCodigoPostal("28002");
        completo = clienteRepository.save(completo);

        return new Scenario(usuario.getId(), completo.getId());
    }

    /**
     * Un ítem manual con IVA; {@code estado} p.ej. {@link com.appgestion.api.constant.PresupuestoEstado}.
     */
    public static String presupuestoJson(long clienteId, String estado, double precioUnitario, double cantidad) {
        String est = estado == null || estado.isBlank() ? "null" : "\"" + estado + "\"";
        return """
                {
                  "clienteId": %d,
                  "items": [{"materialId": null, "tareaManual": "Concepto test", "cantidad": %s, "precioUnitario": %s, "aplicaIva": true}],
                  "ivaHabilitado": true,
                  "estado": %s,
                  "descuentoGlobalPorcentaje": 0.0,
                  "descuentoGlobalFijo": 0.0,
                  "descuentoAntesIva": true,
                  "condicionesActivas": [],
                  "notaAdicional": null
                }
                """.formatted(clienteId, cantidad, precioUnitario, est);
    }
}
