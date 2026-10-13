package com.appgestion.api.dto.request;

import com.appgestion.api.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param totpCode código 2FA; obligatorio solo si la cuenta tiene 2FA activo (SEC-09). Sin él, el servidor
 *                 responde 400 {@code TOTP_REQUERIDO} y NO consume el enlace de restablecimiento.
 */
public record ResetPasswordRequest(
        @NotBlank(message = "El token es obligatorio")
        @Size(max = 256, message = "Token inválido")
        String token,

        @NotBlank(message = "La nueva contraseña es obligatoria")
        @StrongPassword
        String newPassword,

        @Size(max = 16, message = "Código de verificación inválido")
        String totpCode
) {
    public ResetPasswordRequest(String token, String newPassword) {
        this(token, newPassword, null);
    }
}
