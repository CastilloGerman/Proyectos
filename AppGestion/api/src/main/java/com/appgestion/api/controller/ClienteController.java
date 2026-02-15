package com.appgestion.api.controller;

import com.appgestion.api.dto.request.ClienteRequest;
import com.appgestion.api.dto.response.ClienteResponse;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.security.SecurityUtils;
import com.appgestion.api.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;
    private final UsuarioRepository usuarioRepository;

    public ClienteController(ClienteService clienteService, UsuarioRepository usuarioRepository) {
        this.clienteService = clienteService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public List<ClienteResponse> listar() {
        Long usuarioId = SecurityUtils.getCurrentUsuario(usuarioRepository).getId();
        return clienteService.listar(usuarioId);
    }

    @GetMapping("/{id}")
    public ClienteResponse obtenerPorId(@PathVariable Long id) {
        Long usuarioId = SecurityUtils.getCurrentUsuario(usuarioRepository).getId();
        return clienteService.obtenerPorId(id, usuarioId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponse crear(@Valid @RequestBody ClienteRequest request) {
        var usuario = SecurityUtils.getCurrentUsuario(usuarioRepository);
        return clienteService.crear(request, usuario);
    }

    @PutMapping("/{id}")
    public ClienteResponse actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
        Long usuarioId = SecurityUtils.getCurrentUsuario(usuarioRepository).getId();
        return clienteService.actualizar(id, request, usuarioId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        Long usuarioId = SecurityUtils.getCurrentUsuario(usuarioRepository).getId();
        clienteService.eliminar(id, usuarioId);
    }
}
