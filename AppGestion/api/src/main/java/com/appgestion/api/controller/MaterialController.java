package com.appgestion.api.controller;

import com.appgestion.api.dto.request.MaterialRequest;
import com.appgestion.api.dto.response.MaterialResponse;
import com.appgestion.api.service.CurrentUserService;
import com.appgestion.api.service.MaterialService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/materiales")
public class MaterialController {

    private final MaterialService materialService;
    private final CurrentUserService currentUserService;

    public MaterialController(MaterialService materialService, CurrentUserService currentUserService) {
        this.materialService = materialService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<MaterialResponse> listar() {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        return materialService.listar(usuarioId);
    }

    @GetMapping("/top-usados")
    public List<MaterialResponse> topUsados() {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        return materialService.findTop5MasUsados(usuarioId);
    }

    @GetMapping("/{id:\\d+}")
    public MaterialResponse obtenerPorId(@PathVariable Long id) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        return materialService.obtenerPorId(id, usuarioId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialResponse crear(@Valid @RequestBody MaterialRequest request) {
        var usuario = currentUserService.getCurrentUsuario();
        return materialService.crear(request, usuario);
    }

    @PutMapping("/{id}")
    public MaterialResponse actualizar(@PathVariable Long id, @Valid @RequestBody MaterialRequest request) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        return materialService.actualizar(id, request, usuarioId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        materialService.eliminar(id, usuarioId);
    }
}
