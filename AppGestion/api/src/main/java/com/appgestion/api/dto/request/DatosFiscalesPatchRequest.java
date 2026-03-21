package com.appgestion.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos fiscales del emisor para facturación (complementan nombre/NIF/domicilio en {@code empresas}).
 */
public record DatosFiscalesPatchRequest(
        @NotBlank(message = "Indica el régimen de IVA o impuesto aplicable")
        @Size(max = 120)
        String regimenIvaPrincipal,
        @Size(max = 500)
        String descripcionActividad,
        @Size(max = 20)
        String nifIntracomunitario,
        @Size(max = 30)
        String epigrafeIae
) {}
