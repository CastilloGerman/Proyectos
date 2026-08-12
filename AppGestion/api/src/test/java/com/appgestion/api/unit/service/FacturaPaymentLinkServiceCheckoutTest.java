package com.appgestion.api.unit.service;

import com.appgestion.api.constant.FacturaEstadoPago;
import com.appgestion.api.domain.entity.Factura;
import com.appgestion.api.domain.entity.FacturaCobro;
import com.appgestion.api.repository.FacturaCobroRepository;
import com.appgestion.api.repository.FacturaRepository;
import com.appgestion.api.service.FacturaPaymentLinkService;
import com.appgestion.api.service.FacturaResponseMapper;
import com.appgestion.api.service.StripeService;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacturaPaymentLinkServiceCheckoutTest {

    @Mock
    private FacturaRepository facturaRepository;
    @Mock
    private FacturaCobroRepository facturaCobroRepository;
    @Mock
    private StripeService stripeService;
    @Mock
    private FacturaResponseMapper facturaResponseMapper;

    @InjectMocks
    private FacturaPaymentLinkService service;

    @Test
    void registrarPagoDesdeCheckoutSession_marcaPagadaYGuardaCobro() {
        Factura factura = new Factura();
        factura.setId(42L);
        factura.setTotal(121.0);
        factura.setMontoCobrado(0.0);
        factura.setEstadoPago(FacturaEstadoPago.NO_PAGADA);
        factura.setAnulada(false);

        Session session = org.mockito.Mockito.mock(Session.class);
        when(session.getPaymentStatus()).thenReturn("paid");
        when(session.getAmountTotal()).thenReturn(12100L);
        when(session.getId()).thenReturn("cs_test_1");
        when(session.getCreated()).thenReturn(1_700_000_000L);
        when(facturaRepository.findById(42L)).thenReturn(Optional.of(factura));

        service.registrarPagoDesdeCheckoutSession(session, "42");

        ArgumentCaptor<FacturaCobro> cobroCap = ArgumentCaptor.forClass(FacturaCobro.class);
        verify(facturaCobroRepository).save(cobroCap.capture());
        assertThat(cobroCap.getValue().getImporte()).isEqualTo(121.0);
        assertThat(cobroCap.getValue().getMetodo()).isEqualTo("Stripe");

        verify(facturaRepository).save(factura);
        assertThat(factura.getEstadoPago()).isEqualTo(FacturaEstadoPago.PAGADA);
        assertThat(factura.getMontoCobrado()).isEqualTo(121.0);
    }

    @Test
    void registrarPagoDesdeCheckoutSession_yaPagada_noDuplicaCobro() {
        Factura factura = new Factura();
        factura.setId(7L);
        factura.setTotal(50.0);
        factura.setMontoCobrado(50.0);
        factura.setEstadoPago(FacturaEstadoPago.PAGADA);
        factura.setAnulada(false);

        Session session = org.mockito.Mockito.mock(Session.class);
        when(session.getPaymentStatus()).thenReturn("paid");
        when(facturaRepository.findById(7L)).thenReturn(Optional.of(factura));

        service.registrarPagoDesdeCheckoutSession(session, "7");

        verify(facturaCobroRepository, never()).save(any());
        verify(facturaRepository, never()).save(any());
    }
}
