package com.appgestion.api.dto.response;

import java.time.LocalDateTime;

public record ClienteResponse(
        Long id,
        String nombre,
        String telefono,
        String email,
        String direccion,
        String codigoPostal,
        String provincia,
        String pais,
        String dni,
        LocalDateTime fechaCreacion
) {}
