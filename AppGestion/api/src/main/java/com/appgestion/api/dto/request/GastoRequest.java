package com.appgestion.api.dto.request;

import com.appgestion.api.domain.enums.GastoCategoria;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record GastoRequest(
        @NotBlank(message = "El proveedor es obligatorio")
        @Size(max = 200)
        String proveedor,

        @NotBlank(message = "El concepto es obligatorio")
        @Size(max = 500)
        String concepto,

        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        @NotNull(message = "La base imponible es obligatoria")
        @DecimalMin(value = "0", message = "La base imponible debe ser mayor o igual a 0")
        Double baseImponible,

        @NotNull(message = "El tipo de IVA es obligatorio")
        @DecimalMin(value = "0", message = "El tipo de IVA debe ser mayor o igual a 0")
        Double tipoIva,

        @NotNull(message = "La categoría es obligatoria")
        GastoCategoria categoria
) {}
