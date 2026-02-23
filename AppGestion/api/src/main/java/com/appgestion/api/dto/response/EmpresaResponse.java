package com.appgestion.api.dto.response;

public record EmpresaResponse(
        Long id,
        String nombre,
        String direccion,
        String nif,
        String telefono,
        String email,
        String notasPiePresupuesto,
        String notasPieFactura
) {}
