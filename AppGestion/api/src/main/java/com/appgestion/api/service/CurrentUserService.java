package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.security.SecurityUtils;
import org.springframework.stereotype.Service;

/**
 * Servicio para obtener el usuario autenticado actual.
 * Encapsula el acceso al contexto de seguridad y evita que los controladores
 * dependan directamente de UsuarioRepository (principio de inversión de dependencias).
 */
@Service
public class CurrentUserService {

    private final UsuarioRepository usuarioRepository;

    public CurrentUserService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario getCurrentUsuario() {
        return SecurityUtils.getCurrentUsuario(usuarioRepository);
    }
}
