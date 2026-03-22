package com.appgestion.api.service;

import com.appgestion.api.repository.UsuarioSesionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class UsuarioSesionCleanupService {

    private final UsuarioSesionRepository usuarioSesionRepository;

    public UsuarioSesionCleanupService(UsuarioSesionRepository usuarioSesionRepository) {
        this.usuarioSesionRepository = usuarioSesionRepository;
    }

    @Transactional
    public int deleteExpiredBefore(Instant cutoff) {
        return usuarioSesionRepository.deleteByExpiresAtBefore(cutoff);
    }
}
