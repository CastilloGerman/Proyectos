package com.appgestion.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Actualización del perfil del usuario autenticado (nombre visible y teléfono de contacto).
 * El correo no se modifica por este endpoint; requiere flujo de verificación aparte.
 */
public record UpdatePerfilRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
        String telefono
) {}
