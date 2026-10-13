package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Factura;
import com.appgestion.api.domain.entity.FacturaAuditoria;
import com.appgestion.api.domain.entity.FacturaItem;
import com.appgestion.api.repository.FacturaAuditoriaRepository;
import com.appgestion.api.util.OpaqueTokens;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SEC-15: pista de auditoría de cambios en facturas con hash encadenado.
 * <p>
 * Cada alta, edición, anulación o cambio de estado de pago guarda una instantánea de los campos con relevancia
 * fiscal (antes y después). El hash de cada registro incluye el del anterior de la misma factura, de forma que
 * alterar, borrar o reordenar un registro histórico invalida la cadena ({@link #verificarCadena(Long)}).
 * <p>
 * Se ejecuta dentro de la transacción de la operación: si esta se revierte, el registro de auditoría también.
 */
@Service
public class FacturaAuditoriaService {

    public static final String EVENTO_CREADA = "CREADA";
    public static final String EVENTO_EDITADA = "EDITADA";
    public static final String EVENTO_ANULADA = "ANULADA";
    public static final String EVENTO_ESTADO_PAGO = "ESTADO_PAGO";

    /** Hash "anterior" del primer registro de una factura. */
    public static final String GENESIS_HASH = "0".repeat(64);

    private final FacturaAuditoriaRepository repository;
    private final ObjectMapper objectMapper;

    public FacturaAuditoriaService(FacturaAuditoriaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /** Instantánea de los campos fiscalmente relevantes. Llamar ANTES de modificar la factura para obtener el "antes". */
    public Map<String, Object> snapshot(Factura f) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("numeroFactura", f.getNumeroFactura());
        m.put("clienteId", f.getCliente() != null ? f.getCliente().getId() : null);
        m.put("fechaExpedicion", f.getFechaExpedicion() != null ? f.getFechaExpedicion().toString() : null);
        m.put("subtotal", f.getSubtotal());
        m.put("iva", f.getIva());
        m.put("total", f.getTotal());
        m.put("estadoPago", f.getEstadoPago());
        m.put("montoCobrado", f.getMontoCobrado());
        m.put("anulada", Boolean.TRUE.equals(f.getAnulada()));
        List<Map<String, Object>> items = new ArrayList<>();
        if (f.getItems() != null) {
            for (FacturaItem it : f.getItems()) {
                Map<String, Object> im = new LinkedHashMap<>();
                im.put("concepto", it.getTareaManual());
                im.put("cantidad", it.getCantidad());
                im.put("precioUnitario", it.getPrecioUnitario());
                im.put("tipoIva", it.getTipoIva());
                im.put("subtotal", it.getSubtotal());
                items.add(im);
            }
        }
        m.put("items", items);
        return m;
    }

    /**
     * Registra un evento sobre la factura.
     *
     * @param antes instantánea previa al cambio ({@code null} en el alta)
     * @param usuarioId autor del cambio
     */
    @Transactional
    public FacturaAuditoria registrar(Factura factura, String evento, Map<String, Object> antes, Long usuarioId) {
        Map<String, Object> datos = new LinkedHashMap<>();
        if (antes != null) {
            datos.put("antes", antes);
        }
        datos.put("despues", snapshot(factura));

        FacturaAuditoria rec = new FacturaAuditoria();
        rec.setFacturaId(factura.getId());
        rec.setUsuarioId(usuarioId);
        rec.setEvento(evento);
        rec.setCreatedAt(Instant.now().truncatedTo(ChronoUnit.MILLIS));
        rec.setDatosJson(toJson(datos));
        rec.setPrevHash(repository.findTopByFacturaIdOrderByIdDesc(factura.getId())
                .map(FacturaAuditoria::getHash)
                .orElse(GENESIS_HASH));
        rec.setHash(calcularHash(rec));
        return repository.save(rec);
    }

    /** @return {@code true} si la cadena de la factura es íntegra (o no tiene registros). */
    @Transactional(readOnly = true)
    public boolean verificarCadena(Long facturaId) {
        String esperadoPrev = GENESIS_HASH;
        for (FacturaAuditoria rec : repository.findByFacturaIdOrderByIdAsc(facturaId)) {
            if (!esperadoPrev.equals(rec.getPrevHash()) || !calcularHash(rec).equals(rec.getHash())) {
                return false;
            }
            esperadoPrev = rec.getHash();
        }
        return true;
    }

    static String calcularHash(FacturaAuditoria r) {
        String material = String.join("|",
                r.getPrevHash(),
                String.valueOf(r.getFacturaId()),
                String.valueOf(r.getUsuarioId()),
                r.getEvento(),
                r.getCreatedAt().toString(),
                r.getDatosJson());
        return OpaqueTokens.sha256Hex(material);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar la auditoría de factura", e);
        }
    }
}
