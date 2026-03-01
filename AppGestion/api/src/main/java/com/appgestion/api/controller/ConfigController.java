package com.appgestion.api.controller;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.dto.request.EmpresaRequest;
import com.appgestion.api.dto.response.EmpresaResponse;
import com.appgestion.api.service.CurrentUserService;
import com.appgestion.api.service.EmpresaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/config")
public class ConfigController {

    private final EmpresaService empresaService;
    private final CurrentUserService currentUserService;

    public ConfigController(EmpresaService empresaService, CurrentUserService currentUserService) {
        this.empresaService = empresaService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/empresa")
    public EmpresaResponse obtenerEmpresa() {
        Usuario usuario = currentUserService.getCurrentUsuario();
        return empresaService.obtenerPorUsuario(usuario.getId());
    }

    @PutMapping("/empresa")
    public EmpresaResponse guardarEmpresa(@Valid @RequestBody EmpresaRequest request) {
        Usuario usuario = currentUserService.getCurrentUsuario();
        return empresaService.guardar(request, usuario);
    }
}
