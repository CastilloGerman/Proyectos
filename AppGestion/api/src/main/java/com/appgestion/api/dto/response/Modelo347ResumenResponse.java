package com.appgestion.api.dto.response;

import java.util.List;

public record Modelo347ResumenResponse(
        int anio,
        List<Modelo347ClienteResponse> clientes,
        int totalClientesUmbral,
        String umbralEuros,
        String avisoLegal
) {}
