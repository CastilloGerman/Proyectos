package com.appgestion.api.dto.request;

public record EmpresaRequest(
        String nombre,
        String direccion,
        String nif,
        String telefono,
        String email,
        String notasPiePresupuesto,
        String notasPieFactura
) {}
