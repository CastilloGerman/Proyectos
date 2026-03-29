package com.appgestion.api.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Completar datos fiscales: DNI, dirección y CP obligatorios (validados también en servicio).
 */
public record ClienteCompletoRequest(
        @Size(max = 200) String nombre,
        String dni,
        String direccion,
        String codigoPostal,
        @Size(max = 50) String telefono,
        @Size(max = 150) String email,
        @Size(max = 100) String pais,
        @Size(max = 100) String provincia
) {}
