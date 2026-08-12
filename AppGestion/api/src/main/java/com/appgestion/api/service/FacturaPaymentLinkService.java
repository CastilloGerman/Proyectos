package com.appgestion.api.service;

import com.appgestion.api.constant.FacturaEstadoPago;
import com.appgestion.api.domain.entity.Factura;
import com.appgestion.api.domain.entity.FacturaCobro;
import com.appgestion.api.dto.response.FacturaResponse;
import com.appgestion.api.repository.FacturaCobroRepository;
import com.appgestion.api.repository.FacturaRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
public class FacturaPaymentLinkService {

    private static final Logger log = LoggerFactory.getLogger(FacturaPaymentLinkService.class);

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

    /**
     * Aplica el cobro de un Checkout {@code mode=payment} iniciado desde el enlace de factura.
     * Idempotente si la factura ya está marcada como Pagada.
     */
    @Transactional
    public void registrarPagoDesdeCheckoutSession(Session session, String facturaIdMeta) {
        if (session == null || facturaIdMeta == null || facturaIdMeta.isBlank()) {
            return;
        }
        String paymentStatus = session.getPaymentStatus();
        if (paymentStatus != null && !"paid".equalsIgnoreCase(paymentStatus.trim())
                && !"no_payment_required".equalsIgnoreCase(paymentStatus.trim())) {
            log.info("Checkout factura {}: payment_status={} — se ignora hasta cobro confirmado",
                    facturaIdMeta, paymentStatus);
            return;
        }

        final long facturaId;
        try {
            facturaId = Long.parseLong(facturaIdMeta.trim());
        } catch (NumberFormatException ex) {
            log.warn("Checkout factura: factura_id metadata inválido: {}", facturaIdMeta);
            return;
        }

        Factura factura = facturaRepository.findById(facturaId).orElse(null);
        if (factura == null) {
            log.warn("Checkout factura: factura {} no encontrada", facturaId);
            return;
        }
        if (Boolean.TRUE.equals(factura.getAnulada())) {
            log.warn("Checkout factura {}: factura anulada — no se registra cobro", facturaId);
            return;
        }
        if (FacturaEstadoPago.PAGADA.equalsIgnoreCase(
                Optional.ofNullable(factura.getEstadoPago()).orElse("").trim())) {
            return;
        }

        // Preferir amount_total del Checkout; si falta, cerrar por el total de la factura.
        double importe;
        Long amountTotal = session.getAmountTotal();
        if (amountTotal != null && amountTotal > 0) {
            importe = amountTotal / 100.0;
        } else {
            importe = Optional.ofNullable(factura.getTotal()).orElse(0.0);
        }
        if (importe <= 0) {
            log.warn("Checkout factura {}: importe no positivo", facturaId);
            return;
        }

        LocalDate fechaCobro = session.getCreated() != null
                ? Instant.ofEpochSecond(session.getCreated()).atZone(ZoneOffset.UTC).toLocalDate()
                : LocalDate.now(ZoneOffset.UTC);

        FacturaCobro cobro = new FacturaCobro();
        cobro.setFactura(factura);
        cobro.setImporte(importe);
        cobro.setFecha(fechaCobro);
        cobro.setMetodo("Stripe");
        cobro.setNotas("Cobro automático Checkout " + Optional.ofNullable(session.getId()).orElse(""));
        facturaCobroRepository.save(cobro);

        double prev = Optional.ofNullable(factura.getMontoCobrado()).orElse(0.0);
        double nuevoMonto = prev + importe;
        factura.setMontoCobrado(nuevoMonto);
        double total = Optional.ofNullable(factura.getTotal()).orElse(0.0);
        if (nuevoMonto + 0.001 >= total) {
            factura.setEstadoPago(FacturaEstadoPago.PAGADA);
        } else if (nuevoMonto > 0) {
            factura.setEstadoPago(FacturaEstadoPago.PARCIAL);
        }
        facturaRepository.save(factura);
        log.info("Checkout factura {}: cobro {} € registrado (estado={})",
                facturaId, importe, factura.getEstadoPago());
    }
}
