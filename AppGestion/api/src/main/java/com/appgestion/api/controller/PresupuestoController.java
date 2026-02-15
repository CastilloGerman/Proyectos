package com.appgestion.api.controller;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.dto.request.PresupuestoRequest;
import com.appgestion.api.dto.response.PresupuestoResponse;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.security.SecurityUtils;
import com.appgestion.api.service.PresupuestoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/presupuestos")
public class PresupuestoController {

    private final PresupuestoService presupuestoService;
    private final UsuarioRepository usuarioRepository;

    public PresupuestoController(PresupuestoService presupuestoService, UsuarioRepository usuarioRepository) {
        this.presupuestoService = presupuestoService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public List<PresupuestoResponse> listar() {
        Usuario usuario = SecurityUtils.getCurrentUsuario(usuarioRepository);
        return presupuestoService.listar(usuario.getId());
    }

    @GetMapping("/{id}")
    public PresupuestoResponse obtenerPorId(@PathVariable Long id) {
        Long usuarioId = SecurityUtils.getCurrentUsuario(usuarioRepository).getId();
        return presupuestoService.obtenerPorId(id, usuarioId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PresupuestoResponse crear(@Valid @RequestBody PresupuestoRequest request) {
        Usuario usuario = SecurityUtils.getCurrentUsuario(usuarioRepository);
        return presupuestoService.crear(request, usuario);
    }

    @PutMapping("/{id}")
    public PresupuestoResponse actualizar(@PathVariable Long id, @Valid @RequestBody PresupuestoRequest request) {
        Long usuarioId = SecurityUtils.getCurrentUsuario(usuarioRepository).getId();
        return presupuestoService.actualizar(id, request, usuarioId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        Long usuarioId = SecurityUtils.getCurrentUsuario(usuarioRepository).getId();
        presupuestoService.eliminar(id, usuarioId);
    }
}
