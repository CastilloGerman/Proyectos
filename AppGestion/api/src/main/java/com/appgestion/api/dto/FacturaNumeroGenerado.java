package com.appgestion.api.dto;

/**
 * Resultado de la generación atómica del número de factura (FAC-AAAA-NNNN).
 */
public record FacturaNumeroGenerado(String numeroFactura, int anioFactura, int numeroSecuencial) {}
