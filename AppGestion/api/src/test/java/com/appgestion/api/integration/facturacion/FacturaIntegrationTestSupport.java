package com.appgestion.api.integration.facturacion;

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
 * Semilla para tests de facturación: usuario con empresa fiscal mínima y clientes (completo, provisional,
 * mismo NIF que el emisor para casos de error).
 */
public final class FacturaIntegrationTestSupport {

    public static final String EMAIL_USUARIO_FACTURACION = "facturacion-block3@test.local";

    /** NIF emisor (empresa); dígito de control válido para DNI español ({@code 00000000} → {@code T}). */
    public static final String NIF_EMPRESA = "00000000T";

    /** Cliente completo habitual (distinto del emisor). */
    public static final String NIF_CLIENTE_COMPLETO = "00000001R";

    public record Scenario(
            Long usuarioId,
            Long clienteCompletoId,
            Long clienteProvisionalId,
            Long clienteMismoNifQueEmisorId
    ) {
    }

    private FacturaIntegrationTestSupport() {
    }

    public static Scenario seed(
            OrganizationRepository organizationRepository,
            UsuarioRepository usuarioRepository,
            ClienteRepository clienteRepository,
            EmpresaRepository empresaRepository) {
        Organization org = new Organization();
        org.setName("Org facturación block3");
        org = organizationRepository.save(org);

        Usuario usuario = new Usuario();
        usuario.setNombre("Usuario facturación B3");
        usuario.setEmail(EMAIL_USUARIO_FACTURACION);
        usuario.setPasswordHash("$2a$10$dummyhashfordummytestsxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        usuario.setRol("USER");
        usuario.setActivo(true);
        usuario.setOrganization(org);
        usuario = usuarioRepository.save(usuario);

        Empresa empresa = new Empresa();
        empresa.setUsuario(usuario);
        empresa.setNombre("Empresa test B3");
        empresa.setNif(NIF_EMPRESA);
        empresa.setCodigoPostal("28001");
        empresa.setProvincia("Madrid");
        empresa.setPais("España");
        empresaRepository.save(empresa);

        Cliente completo = new Cliente();
        completo.setUsuario(usuario);
        completo.setNombre("Cliente completo B3");
        completo.setEstadoCliente(EstadoCliente.COMPLETO);
        completo.setDni(NIF_CLIENTE_COMPLETO);
        completo.setDireccion("Calle Test 1");
        completo.setCodigoPostal("28002");
        completo = clienteRepository.save(completo);

        Cliente provisional = new Cliente();
        provisional.setUsuario(usuario);
        provisional.setNombre("Cliente provisional B3");
        provisional.setEstadoCliente(EstadoCliente.PROVISIONAL);
        provisional = clienteRepository.save(provisional);

        Cliente mismoNif = new Cliente();
        mismoNif.setUsuario(usuario);
        mismoNif.setNombre("Cliente mismo NIF emisor B3");
        mismoNif.setEstadoCliente(EstadoCliente.COMPLETO);
        mismoNif.setDni(NIF_EMPRESA);
        mismoNif.setDireccion("Calle Test 2");
        mismoNif.setCodigoPostal("28003");
        mismoNif = clienteRepository.save(mismoNif);

        return new Scenario(
                usuario.getId(),
                completo.getId(),
                provisional.getId(),
                mismoNif.getId());
    }
}
