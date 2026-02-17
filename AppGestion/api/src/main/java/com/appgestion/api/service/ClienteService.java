package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Cliente;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.dto.request.ClienteRequest;
import com.appgestion.api.dto.response.ClienteResponse;
import com.appgestion.api.repository.ClienteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public List<ClienteResponse> listar(Long usuarioId) {
        return clienteRepository.findByUsuarioId(usuarioId).stream()
                .map(this::toResponse)
                .toList();
    }

    public ClienteResponse obtenerPorId(Long id, Long usuarioId) {
        Cliente cliente = clienteRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
        return toResponse(cliente);
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest request, Usuario usuario) {
        Cliente cliente = new Cliente();
        cliente.setUsuario(usuario);
        mapRequestToEntity(request, cliente);
        cliente = clienteRepository.save(cliente);
        return toResponse(cliente);
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest request, Long usuarioId) {
        Cliente cliente = clienteRepository.findByIdAndUsuarioId(
                Objects.requireNonNull(id), Objects.requireNonNull(usuarioId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
        mapRequestToEntity(request, Objects.requireNonNull(cliente));
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

    private void mapRequestToEntity(ClienteRequest request, Cliente cliente) {
        cliente.setNombre(request.nombre());
        cliente.setTelefono(request.telefono() != null ? request.telefono() : "");
        cliente.setEmail(request.email() != null ? request.email() : "");
        cliente.setDireccion(request.direccion() != null ? request.direccion() : "");
        cliente.setDni(request.dni() != null ? request.dni() : "");
    }

    private ClienteResponse toResponse(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getTelefono(),
                cliente.getEmail(),
                cliente.getDireccion(),
                cliente.getDni(),
                cliente.getFechaCreacion()
        );
    }
}
