package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Factura;
import com.appgestion.api.domain.entity.FacturaCobro;
import com.appgestion.api.domain.enums.TipoFactura;
import com.appgestion.api.dto.response.FacturaCobroResponse;
import com.appgestion.api.dto.response.FacturaItemResponse;
import com.appgestion.api.dto.response.FacturaResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FacturaResponseMapper {

    public FacturaResponse toResponse(Factura factura, List<FacturaCobro> cobrosList) {
        return toResponse(factura, cobrosList, List.of(), true);
    }

    public FacturaResponse toResponse(Factura factura, List<FacturaCobro> cobrosList, List<String> avisos) {
        return toResponse(factura, cobrosList, avisos, true);
    }

    /** Listados y dashboard: no hidrata partidas ni materiales. */
    public FacturaResponse toListResponse(Factura factura) {
        return toResponse(factura, List.of(), List.of(), false);
    }

    public FacturaResponse toResponse(Factura factura, List<FacturaCobro> cobrosList, List<String> avisos, boolean includeItems) {
        List<FacturaItemResponse> items = includeItems ? factura.getItems().stream()
                .map(item -> new FacturaItemResponse(
                        item.getId(),
                        item.getMaterial() != null ? item.getMaterial().getId() : null,
                        item.getEsTareaManual() != null && item.getEsTareaManual() ? item.getTareaManual() : (item.getMaterial() != null ? item.getMaterial().getNombre() : ""),
                        item.getEsTareaManual() != null && item.getEsTareaManual(),
                        item.getCantidad(),
                        item.getPrecioUnitario(),
                        item.getSubtotal(),
                        Boolean.TRUE.equals(item.getAplicaIva()) || item.getAplicaIva() == null,
                        item.getTipoIva() != null ? item.getTipoIva() : (Boolean.FALSE.equals(item.getAplicaIva()) ? 0 : 21)
                ))
                .toList() : List.of();

        List<FacturaCobroResponse> cobros = cobrosList.stream()
                .map(c -> new FacturaCobroResponse(
                        c.getId(), c.getImporte(), c.getFecha(), c.getMetodo(), c.getNotas(), c.getCreatedAt()))
                .toList();

        TipoFactura tf = factura.getTipoFactura() != null ? factura.getTipoFactura() : TipoFactura.NORMAL;
        Factura ref = factura.getFacturaAnticipoReferencia();
        Long facturaAnticipoId = ref != null ? ref.getId() : null;
        String numeroFacturaAnticipo = ref != null ? ref.getNumeroFactura() : null;
        Double importeAnticipoDescontado = factura.getImporteAnticipoDescontado() != null
                ? factura.getImporteAnticipoDescontado().doubleValue() : null;

        return new FacturaResponse(
                factura.getId(),
                factura.getNumeroFactura(),
                factura.getCliente().getId(),
                factura.getCliente().getNombre(),
                factura.getCliente().getEmail(),
                factura.getPresupuesto() != null ? factura.getPresupuesto().getId() : null,
                factura.getFechaCreacion(),
                factura.getFechaExpedicion(),
                factura.getFechaOperacion(),
                factura.getFechaVencimiento(),
                factura.getSubtotal(),
                factura.getIva(),
                factura.getTotal(),
                factura.getIvaHabilitado(),
                factura.getRegimenFiscal(),
                factura.getCondicionesPago(),
                factura.getMetodoPago(),
                factura.getEstadoPago(),
                factura.getMontoCobrado(),
                factura.getNotas(),
                items,
                factura.getPaymentLinkUrl(),
                tf.name(),
                facturaAnticipoId,
                numeroFacturaAnticipo,
                importeAnticipoDescontado,
                factura.getAnioFactura(),
                factura.getNumeroSecuencial(),
                factura.getAnulada(),
                factura.getFechaAnulacion(),
                factura.getMotivoAnulacion(),
                cobros,
                avisos == null ? List.of() : List.copyOf(avisos)
        );
    }
}
