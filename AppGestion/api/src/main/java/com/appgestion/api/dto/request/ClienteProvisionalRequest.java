package com.appgestion.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClienteProvisionalRequest(
        @NotBlank @Size(max = 200) String nombre
) {}
