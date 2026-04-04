package com.appgestion.api.service.factura;

/**
 * Número de factura ya validado listo para persistir en creación.
 */
public record FacturaNumeroResuelto(String numeroFactura, int anioFactura, int numeroSecuencial) {
}
