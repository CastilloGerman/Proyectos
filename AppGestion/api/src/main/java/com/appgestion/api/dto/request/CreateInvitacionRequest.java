package com.appgestion.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Invitación tipo referido: solo email. El nuevo usuario es cuenta independiente (USER);
 * permisos de edición los marca la suscripción / periodo de prueba, no un rol en el enlace.
 */
public record CreateInvitacionRequest(
        @NotBlank @Email String email
) {}
