package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Factura;
import com.appgestion.api.dto.response.FacturaResponse;
import com.appgestion.api.repository.FacturaCobroRepository;
import com.appgestion.api.repository.FacturaRepository;
import com.stripe.exception.StripeException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class FacturaPaymentLinkService {

    private final FacturaRepository facturaRepository;
    private final FacturaCobroRepository facturaCobroRepository;
    private final StripeService stripeService;
    private final FacturaResponseMapper facturaResponseMapper;

    public FacturaPaymentLinkService(FacturaRepository facturaRepository,
                                     FacturaCobroRepository facturaCobroRepository,
                                     StripeService stripeService,
                                     FacturaResponseMapper facturaResponseMapper) {
        this.facturaRepository = facturaRepository;
        this.facturaCobroRepository = facturaCobroRepository;
        this.stripeService = stripeService;
        this.facturaResponseMapper = facturaResponseMapper;
    }

    @Transactional
    public FacturaResponse generarPaymentLink(Long facturaId, Long usuarioId) {
        Factura factura = facturaRepository.findByIdAndUsuarioId(facturaId, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada"));
        if (Boolean.TRUE.equals(factura.getAnulada())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede generar enlace de pago para una factura anulada");
        }
        if (factura.getPaymentLinkUrl() != null && !factura.getPaymentLinkUrl().isBlank()) {
            return facturaResponseMapper.toResponse(factura,
                    facturaCobroRepository.findByFacturaIdOrderByFechaDescCreatedAtDesc(factura.getId()));
        }
        double total = Optional.ofNullable(factura.getTotal()).orElse(0.0);
        long cents = Math.round(total * 100);
        if (cents < 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Importe de factura demasiado bajo para Payment Link");
        }
        try {
            PaymentLinkResult pl = stripeService.createFacturaCheckoutUrl(
                    factura.getNumeroFactura(), cents, factura.getId().toString());
            factura.setPaymentLinkId(pl.id());
            factura.setPaymentLinkUrl(pl.url());
            facturaRepository.save(factura);
        } catch (StripeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe: " + e.getMessage());
        }
        return facturaResponseMapper.toResponse(factura,
                facturaCobroRepository.findByFacturaIdOrderByFechaDescCreatedAtDesc(factura.getId()));
    }
}
