package com.appgestion.api.controller;

import com.appgestion.api.dto.request.GastoRequest;
import com.appgestion.api.dto.response.GastoResponse;
import com.appgestion.api.service.CurrentUserService;
import com.appgestion.api.service.GastoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/gastos")
public class GastoController {

    private final GastoService gastoService;
    private final CurrentUserService currentUserService;

    public GastoController(GastoService gastoService, CurrentUserService currentUserService) {
        this.gastoService = gastoService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<GastoResponse> listar() {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        return gastoService.listar(usuarioId);
    }

    @GetMapping("/{id:\\d+}")
    public GastoResponse obtenerPorId(@PathVariable Long id) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        return gastoService.obtenerPorId(id, usuarioId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GastoResponse crear(@Valid @RequestBody GastoRequest request) {
        var usuario = currentUserService.getCurrentUsuario();
        return gastoService.crear(request, usuario);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public GastoResponse actualizar(@PathVariable Long id, @Valid @RequestBody GastoRequest request) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        return gastoService.actualizar(id, request, usuarioId);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        gastoService.eliminar(id, usuarioId);
    }
}
