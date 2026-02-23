package com.appgestion.api.controller;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.dto.request.EmpresaRequest;
import com.appgestion.api.dto.response.EmpresaResponse;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.security.SecurityUtils;
import com.appgestion.api.service.EmpresaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/config")
public class ConfigController {

    private final EmpresaService empresaService;
    private final UsuarioRepository usuarioRepository;

    public ConfigController(EmpresaService empresaService, UsuarioRepository usuarioRepository) {
        this.empresaService = empresaService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/empresa")
    public EmpresaResponse obtenerEmpresa() {
        Usuario usuario = SecurityUtils.getCurrentUsuario(usuarioRepository);
        return empresaService.obtenerPorUsuario(usuario.getId());
    }

    @PutMapping("/empresa")
    public EmpresaResponse guardarEmpresa(@Valid @RequestBody EmpresaRequest request) {
        Usuario usuario = SecurityUtils.getCurrentUsuario(usuarioRepository);
        return empresaService.guardar(request, usuario);
    }
}
