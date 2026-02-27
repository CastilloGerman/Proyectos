package com.appgestion.api.dto.response;

public record EmpresaResponse(
        Long id,
        String nombre,
        String direccion,
        String codigoPostal,
        String provincia,
        String pais,
        String nif,
        String telefono,
        String email,
        String notasPiePresupuesto,
        String notasPieFactura,
        String mailHost,
        Integer mailPort,
        String mailUsername,
        Boolean mailConfigurado
) {}
