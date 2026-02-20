package com.appgestion.api.controller;

import com.appgestion.api.dto.request.MaterialRequest;
import com.appgestion.api.dto.response.MaterialResponse;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.security.SecurityUtils;
import com.appgestion.api.service.MaterialService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/materiales")
public class MaterialController {

    private final MaterialService materialService;
    private final UsuarioRepository usuarioRepository;

    public MaterialController(MaterialService materialService, UsuarioRepository usuarioRepository) {
        this.materialService = materialService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public List<MaterialResponse> listar() {
        Long usuarioId = SecurityUtils.getCurrentUsuario(usuarioRepository).getId();
        return materialService.listar(usuarioId);
    }

    @GetMapping("/{id}")
    public MaterialResponse obtenerPorId(@PathVariable Long id) {
        Long usuarioId = SecurityUtils.getCurrentUsuario(usuarioRepository).getId();
        return materialService.obtenerPorId(id, usuarioId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialResponse crear(@Valid @RequestBody MaterialRequest request) {
        var usuario = SecurityUtils.getCurrentUsuario(usuarioRepository);
        return materialService.crear(request, usuario);
    }

    @PutMapping("/{id}")
    public MaterialResponse actualizar(@PathVariable Long id, @Valid @RequestBody MaterialRequest request) {
        Long usuarioId = SecurityUtils.getCurrentUsuario(usuarioRepository).getId();
        return materialService.actualizar(id, request, usuarioId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        Long usuarioId = SecurityUtils.getCurrentUsuario(usuarioRepository).getId();
        materialService.eliminar(id, usuarioId);
    }
}
