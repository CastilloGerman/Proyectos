package com.appgestion.api.controller;

import com.appgestion.api.dto.request.ClienteCompletoRequest;
import com.appgestion.api.dto.request.ClienteProvisionalRequest;
import com.appgestion.api.dto.request.ClienteRequest;
import com.appgestion.api.dto.response.ClientePanelResponse;
import com.appgestion.api.dto.response.ClienteResponse;
import com.appgestion.api.service.ClientePanelService;
import com.appgestion.api.service.ClienteService;
import com.appgestion.api.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;
    private final ClientePanelService clientePanelService;
    private final CurrentUserService currentUserService;

    public ClienteController(
            ClienteService clienteService,
            ClientePanelService clientePanelService,
            CurrentUserService currentUserService
    ) {
        this.clienteService = clienteService;
        this.clientePanelService = clientePanelService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<ClienteResponse> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String estado
    ) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        return clienteService.listar(usuarioId, q, estado);
    }

    @GetMapping("/{id}")
    public ClienteResponse obtenerPorId(@PathVariable Long id) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        return clienteService.obtenerPorId(id, usuarioId);
    }

    @GetMapping("/{id}/panel")
    public ClientePanelResponse panel(@PathVariable Long id) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        return clientePanelService.obtenerPanel(id, usuarioId);
    }

    @PostMapping("/provisional")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponse crearProvisional(@Valid @RequestBody ClienteProvisionalRequest request) {
        var usuario = currentUserService.getCurrentUsuario();
        return clienteService.crearClienteProvisional(request.nombre(), usuario);
    }

    @PutMapping("/{id}/completar")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ClienteResponse completar(@PathVariable Long id, @Valid @RequestBody ClienteCompletoRequest request) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        return clienteService.completarCliente(id, request, usuarioId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponse crear(@Valid @RequestBody ClienteRequest request) {
        var usuario = currentUserService.getCurrentUsuario();
        return clienteService.crear(request, usuario);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ClienteResponse actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        return clienteService.actualizar(id, request, usuarioId);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        clienteService.eliminar(id, usuarioId);
    }
}
