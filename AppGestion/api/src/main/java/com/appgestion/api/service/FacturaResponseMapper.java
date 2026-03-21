package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Factura;
import com.appgestion.api.domain.entity.FacturaCobro;
import com.appgestion.api.dto.response.FacturaCobroResponse;
import com.appgestion.api.dto.response.FacturaItemResponse;
import com.appgestion.api.dto.response.FacturaResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FacturaResponseMapper {

    public FacturaResponse toResponse(Factura factura, List<FacturaCobro> cobrosList) {
        List<FacturaItemResponse> items = factura.getItems().stream()
                .map(item -> new FacturaItemResponse(
                        item.getId(),
                        item.getMaterial() != null ? item.getMaterial().getId() : null,
                        item.getEsTareaManual() != null && item.getEsTareaManual() ? item.getTareaManual() : (item.getMaterial() != null ? item.getMaterial().getNombre() : ""),
                        item.getEsTareaManual() != null && item.getEsTareaManual(),
                        item.getCantidad(),
                        item.getPrecioUnitario(),
                        item.getSubtotal(),
                        Boolean.TRUE.equals(item.getAplicaIva()) || item.getAplicaIva() == null
                ))
                .toList();

        List<FacturaCobroResponse> cobros = cobrosList.stream()
                .map(c -> new FacturaCobroResponse(
                        c.getId(), c.getImporte(), c.getFecha(), c.getMetodo(), c.getNotas(), c.getCreatedAt()))
                .toList();

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
                cobros
        );
    }
}
