package com.appgestion.api.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public record RespuestaClienteRequest(
        @NotBlank(message = "Opción no válida")
        @Pattern(regexp = "INTERESA|DUDAS", message = "Opción no válida")
        String opcion,
        @Size(max = 500, message = "El mensaje no puede superar 500 caracteres")
        String mensaje
) {}
