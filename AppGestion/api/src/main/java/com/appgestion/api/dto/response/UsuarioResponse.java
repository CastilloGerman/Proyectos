package com.appgestion.api.dto.response;

import java.time.LocalDateTime;

public record UsuarioResponse(
        Long id,
        String nombre,
        String email,
        String rol,
        Boolean activo,
        LocalDateTime fechaCreacion
) {}
