package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Cliente;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.EstadoCliente;
import com.appgestion.api.dto.request.ClienteCompletoRequest;
import com.appgestion.api.dto.request.ClienteRequest;
import com.appgestion.api.dto.response.ClienteResponse;
import com.appgestion.api.repository.ClienteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ClienteService {

    /** Mensaje legal al intentar facturar con cliente sin datos fiscales completos. */
    public static final String MSG_FACTURA_CLIENTE_INCOMPLETO =
            "Para emitir una factura necesitas completar los datos fiscales del cliente: DNI, dirección y código postal.";

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public List<ClienteResponse> listar(Long usuarioId, String q, String estado) {
        List<Cliente> base;
        if (estado != null && !estado.isBlank()) {
            base = clienteRepository.findByUsuarioIdAndEstadoCliente(usuarioId, parseEstadoFiltro(estado));
        } else {
            base = clienteRepository.findByUsuarioId(usuarioId);
        }
        var stream = base.stream().map(this::toResponse);
        if (q != null && !q.isBlank()) {
            String lower = q.strip().toLowerCase();
            stream = stream.filter(c ->
                    (c.nombre() != null && c.nombre().toLowerCase().contains(lower)) ||
                    (c.email() != null && c.email().toLowerCase().contains(lower)) ||
                    (c.dni() != null && c.dni().toLowerCase().contains(lower))
            );
        }
        return stream.toList();
    }

    public long contarPorEstado(Long usuarioId, EstadoCliente estado) {
        return clienteRepository.countByUsuarioIdAndEstadoCliente(usuarioId, estado);
    }

    public ClienteResponse obtenerPorId(Long id, Long usuarioId) {
        Cliente cliente = clienteRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
        return toResponse(cliente);
    }

    @Transactional
    public ClienteResponse crearClienteProvisional(String nombre, Usuario usuario) {
        String n = nombre != null ? nombre.strip() : "";
        if (n.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del cliente es obligatorio");
        }
        Cliente cliente = new Cliente();
        cliente.setUsuario(usuario);
        cliente.setNombre(n);
        cliente.setEstadoCliente(EstadoCliente.PROVISIONAL);
        cliente = clienteRepository.save(cliente);
        return toResponse(cliente);
    }

    @Transactional
    public ClienteResponse completarCliente(Long id, ClienteCompletoRequest request, Long usuarioId) {
        Cliente cliente = clienteRepository.findByIdAndUsuarioId(
                        Objects.requireNonNull(id), Objects.requireNonNull(usuarioId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));

        List<String> errores = new ArrayList<>();
        String dni = request.dni() != null ? request.dni().strip() : "";
        String dir = request.direccion() != null ? request.direccion().strip() : "";
        String cp = request.codigoPostal() != null ? request.codigoPostal().strip() : "";
        if (dni.isEmpty()) {
            errores.add("DNI / NIF: obligatorio");
        }
        if (dir.isEmpty()) {
            errores.add("Dirección: obligatoria");
        }
        if (cp.isEmpty()) {
            errores.add("Código postal: obligatorio");
        }
        if (!errores.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join(" ", errores));
        }

        if (request.nombre() != null && !request.nombre().isBlank()) {
            cliente.setNombre(request.nombre().strip());
        }
        cliente.setDni(dni);
        cliente.setDireccion(dir);
        cliente.setCodigoPostal(cp);
        cliente.setTelefono(request.telefono() != null ? request.telefono().strip() : "");
        cliente.setEmail(request.email() != null ? request.email().strip() : "");
        cliente.setProvincia(request.provincia() != null && !request.provincia().isBlank() ? request.provincia().strip() : null);
        cliente.setPais(request.pais() != null && !request.pais().isBlank() ? request.pais().strip() : "España");
        cliente.setEstadoCliente(EstadoCliente.COMPLETO);
        cliente = clienteRepository.save(cliente);
        return toResponse(cliente);
    }

    /**
     * Debe ejecutarse antes de crear/actualizar facturas: el cliente debe tener estado COMPLETO.
     */
    public void validarClienteParaFactura(Long idCliente, Long usuarioId) {
        Cliente cliente = clienteRepository.findByIdAndUsuarioId(
                        Objects.requireNonNull(idCliente), Objects.requireNonNull(usuarioId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
        if (cliente.getEstadoCliente() == EstadoCliente.PROVISIONAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MSG_FACTURA_CLIENTE_INCOMPLETO);
        }
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest request, Usuario usuario) {
        Cliente cliente = new Cliente();
        cliente.setUsuario(usuario);
        mapRequestToEntity(request, cliente);
        aplicarEstadoSegunDatosFiscales(cliente);
        cliente = clienteRepository.save(cliente);
        return toResponse(cliente);
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest request, Long usuarioId) {
        Cliente cliente = clienteRepository.findByIdAndUsuarioId(
                Objects.requireNonNull(id), Objects.requireNonNull(usuarioId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
        mapRequestToEntity(request, Objects.requireNonNull(cliente));
        aplicarEstadoSegunDatosFiscales(cliente);
        cliente = clienteRepository.save(cliente);
        return toResponse(cliente);
    }

    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        if (!clienteRepository.existsByIdAndUsuarioId(Objects.requireNonNull(id), Objects.requireNonNull(usuarioId))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado");
        }
        clienteRepository.deleteById(Objects.requireNonNull(id));
    }

    private static void aplicarEstadoSegunDatosFiscales(Cliente cliente) {
        if (datosFiscalesCompletos(cliente.getDni(), cliente.getDireccion(), cliente.getCodigoPostal())) {
            cliente.setEstadoCliente(EstadoCliente.COMPLETO);
        } else {
            cliente.setEstadoCliente(EstadoCliente.PROVISIONAL);
        }
    }

    private static boolean datosFiscalesCompletos(String dni, String direccion, String codigoPostal) {
        return dni != null && !dni.isBlank()
                && direccion != null && !direccion.isBlank()
                && codigoPostal != null && !codigoPostal.isBlank();
    }

    private EstadoCliente parseEstadoFiltro(String raw) {
        try {
            return EstadoCliente.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "estado debe ser PROVISIONAL o COMPLETO");
        }
    }

    private void mapRequestToEntity(ClienteRequest request, Cliente cliente) {
        cliente.setNombre(request.nombre());
        cliente.setTelefono(request.telefono() != null ? request.telefono() : "");
        cliente.setEmail(request.email() != null ? request.email() : "");
        cliente.setDireccion(request.direccion() != null ? request.direccion() : "");
        cliente.setCodigoPostal(request.codigoPostal());
        cliente.setProvincia(request.provincia());
        cliente.setPais(request.pais() != null ? request.pais() : "España");
        cliente.setDni(request.dni() != null ? request.dni() : "");
    }

    private ClienteResponse toResponse(Cliente cliente) {
        EstadoCliente ec = cliente.getEstadoCliente() != null ? cliente.getEstadoCliente() : EstadoCliente.PROVISIONAL;
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getTelefono(),
                cliente.getEmail(),
                cliente.getDireccion(),
                cliente.getCodigoPostal(),
                cliente.getProvincia(),
                cliente.getPais(),
                cliente.getDni(),
                cliente.getFechaCreacion(),
                ec.name()
        );
    }
}
