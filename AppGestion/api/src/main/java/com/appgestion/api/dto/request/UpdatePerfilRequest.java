package com.appgestion.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Actualización del perfil del usuario autenticado.
 * El correo no se modifica por este endpoint; requiere flujo de verificación aparte.
 */
public record UpdatePerfilRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
        String telefono,

        @Past(message = "La fecha de nacimiento no puede ser futura")
        LocalDate fechaNacimiento,

        @Size(max = 32, message = "Valor de género no válido")
        String genero,

        @Size(min = 2, max = 2, message = "El código de país debe tener 2 letras")
        String nacionalidadIso,

        @Size(min = 2, max = 2, message = "El código de país debe tener 2 letras")
        String paisResidenciaIso
) {}
