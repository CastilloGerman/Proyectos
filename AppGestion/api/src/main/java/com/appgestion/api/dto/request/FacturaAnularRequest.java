package com.appgestion.api.dto.request;

import jakarta.validation.constraints.Size;

public record FacturaAnularRequest(
        @Size(max = 255) String motivo
) {}
