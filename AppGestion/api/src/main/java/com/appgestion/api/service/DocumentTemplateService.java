package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Cliente;
import com.appgestion.api.domain.entity.Presupuesto;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

/**
 * Sustituye marcadores tipo {@code {{cliente.nombre}}} en textos de plantilla para PDFs.
 * Solo texto plano; no interpreta HTML.
 */
public final class DocumentTemplateService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("es-ES"));
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-ES"));

    private DocumentTemplateService() {}

    public static String expandirPresupuesto(String plantilla, Presupuesto p) {
        if (plantilla == null || plantilla.isBlank()) {
            return "";
        }
        Cliente c = p.getCliente();
        String clienteNombre = c != null && c.getNombre() != null ? c.getNombre() : "";
        String clienteEmail = c != null && c.getEmail() != null ? c.getEmail() : "";
        String clienteTelefono = c != null && c.getTelefono() != null ? c.getTelefono() : "";
        String clienteDireccion = c != null && c.getDireccion() != null ? c.getDireccion() : "";
        String idStr = p.getId() != null ? String.valueOf(p.getId()) : "";
        String sub = formatMoney(p.getSubtotal());
        String iva = formatMoney(p.getIva());
        String total = formatMoney(p.getTotal());
        String estado = p.getEstado() != null ? p.getEstado() : "";
        String fecha = p.getFechaCreacion() != null ? p.getFechaCreacion().format(FECHA) : "";
        String fechaHora = p.getFechaCreacion() != null ? p.getFechaCreacion().format(FECHA_HORA) : "";

        return plantilla
                .replace("{{cliente.nombre}}", clienteNombre)
                .replace("{{cliente.email}}", clienteEmail)
                .replace("{{cliente.telefono}}", clienteTelefono)
                .replace("{{cliente.direccion}}", clienteDireccion)
                .replace("{{presupuesto.id}}", idStr)
                .replace("{{fecha}}", fecha)
                .replace("{{fecha_hora}}", fechaHora)
                .replace("{{subtotal}}", sub)
                .replace("{{iva}}", iva)
                .replace("{{total}}", total)
                .replace("{{estado}}", estado);
    }

    private static String formatMoney(Double v) {
        double d = Optional.ofNullable(v).orElse(0.0);
        return String.format(Locale.forLanguageTag("es-ES"), "%.2f €", d);
    }
}
