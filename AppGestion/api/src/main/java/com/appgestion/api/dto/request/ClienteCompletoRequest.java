package com.appgestion.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Completar datos fiscales: DNI, dirección y CP obligatorios (validados también en servicio).
 */
public record ClienteCompletoRequest(
        @Size(max = 200) String nombre,
        String dni,
        String direccion,
        @Pattern(regexp = "^[0-9]{5}$", message = "El código postal debe tener 5 dígitos")
        String codigoPostal,
        @Size(max = 50) String telefono,
        @Email(message = "El email no tiene formato válido")
        @Size(max = 150) String email,
        @Size(max = 100) String pais,
        @Size(max = 100) String provincia
) {}
