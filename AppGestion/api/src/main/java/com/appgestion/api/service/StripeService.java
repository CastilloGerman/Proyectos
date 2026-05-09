package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.dto.SubscriptionBillingPeriod;
import com.appgestion.api.dto.response.SubscriptionInvoiceDto;
import com.appgestion.api.repository.UsuarioRepository;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceCollection;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.InvoiceListParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionCreateParams.LineItem;
import com.stripe.param.checkout.SessionCreateParams.Mode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeService {

    private static final Logger log = LoggerFactory.getLogger(StripeService.class);

    private final UsuarioRepository usuarioRepository;

    public StripeService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Value("${stripe.price-id-monthly}")
    private String priceIdMonthly;

    @Value("${stripe.price-id-yearly:}")
    private String priceIdYearly;

    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    @Value("${stripe.subscription-cancel-url}")
    private String subscriptionCancelUrl;

    @Value("${stripe.portal-return-url}")
    private String portalReturnUrl;

    @Value("${stripe.checkout-automatic-tax-enabled:false}")
    private boolean checkoutAutomaticTaxEnabled;

    public String createCheckoutSession(Usuario usuario, SubscriptionBillingPeriod billingPeriod)
            throws StripeException {
        return createCheckoutSession(usuario, billingPeriod, false);
    }

    /**
     * Si {@code staleRecoveryAttempt} es {@code false} y Stripe indica que el {@code customer} guardado no existe
     * (p. ej. cambio de cuenta Stripe o modo test/live), se borra el ID huérfano y se reintenta una vez con un cliente nuevo.
     */
    private String createCheckoutSession(
            Usuario usuario, SubscriptionBillingPeriod billingPeriod, boolean staleRecoveryAttempt)
            throws StripeException {
        SubscriptionBillingPeriod period =
                billingPeriod != null ? billingPeriod : SubscriptionBillingPeriod.MONTHLY;
        final String priceId;
        if (period == SubscriptionBillingPeriod.YEARLY) {
            priceId = priceIdYearly;
        } else {
            priceId = priceIdMonthly;
        }
        if (priceId == null || priceId.isBlank()) {
            throw new IllegalStateException(
                    period == SubscriptionBillingPeriod.YEARLY
                            ? "Falta STRIPE_PRICE_YEARLY en la configuración del servidor (precio anual Stripe)."
                            : "Falta STRIPE_PRICE_MONTHLY en la configuración del servidor (precio recurrente Stripe).");
        }
        if (successUrl == null
                || successUrl.isBlank()
                || subscriptionCancelUrl == null
                || subscriptionCancelUrl.isBlank()) {
            throw new IllegalStateException(
                    "Faltan STRIPE_SUCCESS_URL y/o URL de cancelación de suscripción (STRIPE_SUBSCRIPTION_CANCEL_URL / stripe.subscription-cancel-url) en la configuración del servidor.");
        }
        String customerId = usuario.getStripeCustomerId();
        if (customerId == null || customerId.isBlank()) {
            customerId = createStripeCustomer(usuario);
            usuario.setStripeCustomerId(customerId);
            usuarioRepository.save(usuario);
        }

        Map<String, String> metadata = new HashMap<>();
        metadata.put("usuario_id", usuario.getId().toString());

        SessionCreateParams.Builder sessionBuilder = SessionCreateParams.builder()
                .setMode(Mode.SUBSCRIPTION)
                .setCustomer(customerId)
                .addLineItem(
                        LineItem.builder()
                                .setPrice(priceId)
                                .setQuantity(1L)
                                .build())
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(subscriptionCancelUrl)
                .putAllMetadata(metadata)
                .setSubscriptionData(
                        SessionCreateParams.SubscriptionData.builder()
                                .putMetadata("usuario_id", usuario.getId().toString())
                                .build());

        if (checkoutAutomaticTaxEnabled) {
            sessionBuilder
                    .setAutomaticTax(
                            SessionCreateParams.AutomaticTax.builder().setEnabled(true).build())
                    .setCustomerUpdate(
                            SessionCreateParams.CustomerUpdate.builder()
                                    .setAddress(SessionCreateParams.CustomerUpdate.Address.AUTO)
                                    .build());
        }

        SessionCreateParams params = sessionBuilder.build();

        try {
            Session checkoutSession = Session.create(params);
            return checkoutSession.getUrl();
        } catch (InvalidRequestException e) {
            if (!staleRecoveryAttempt
                    && "resource_missing".equals(e.getCode())
                    && "customer".equals(e.getParam())) {
                log.warn(
                        "Stripe customer huérfano ({}), recreando cliente para usuario {}",
                        e.getMessage(),
                        usuario.getId());
                usuario.setStripeCustomerId(null);
                usuarioRepository.save(usuario);
                return createCheckoutSession(usuario, billingPeriod, true);
            }
            throw e;
        }
    }

    /**
     * Facturas de suscripción asociadas al cliente Stripe (más recientes primero).
     */
    public List<SubscriptionInvoiceDto> listCustomerInvoices(String stripeCustomerId, int limit)
            throws StripeException {
        if (stripeCustomerId == null || stripeCustomerId.isBlank()) {
            return List.of();
        }
        int lim = Math.min(Math.max(limit, 1), 100);
        InvoiceListParams params = InvoiceListParams.builder()
                .setCustomer(stripeCustomerId)
                .setLimit((long) lim)
                .build();
        InvoiceCollection collection = Invoice.list(params);
        List<SubscriptionInvoiceDto> out = new ArrayList<>();
        for (Invoice inv : collection.getData()) {
            Long amountDue = inv.getAmountDue();
            Long amountPaid = inv.getAmountPaid();
            String currency = inv.getCurrency();
            Long created = inv.getCreated();
            long dueCents = amountDue == null ? 0L : amountDue;
            long paidCents = amountPaid == null ? 0L : amountPaid;
            long createdUnix = created == null ? 0L : created;
            out.add(new SubscriptionInvoiceDto(
                    inv.getId(),
                    inv.getNumber(),
                    inv.getStatus() != null ? inv.getStatus() : "",
                    dueCents,
                    paidCents,
                    currency != null ? currency : "eur",
                    createdUnix,
                    inv.getInvoicePdf(),
                    inv.getHostedInvoiceUrl()));
        }
        return out;
    }

    public String createBillingPortalSession(String stripeCustomerId) throws StripeException {
        com.stripe.param.billingportal.SessionCreateParams params =
                com.stripe.param.billingportal.SessionCreateParams.builder()
                        .setCustomer(stripeCustomerId)
                        .setReturnUrl(portalReturnUrl)
                        .build();

        com.stripe.model.billingportal.Session portalSession =
                com.stripe.model.billingportal.Session.create(params);
        return portalSession.getUrl();
    }

    /**
     * Sesión de pago única (Checkout) para cobrar una factura; devuelve URL pública.
     */
    public PaymentLinkResult createFacturaCheckoutUrl(
            String numeroFactura, long amountCents, String facturaIdMeta) throws StripeException {
        if (amountCents < 50) {
            throw new IllegalArgumentException("Importe demasiado bajo para el checkout");
        }
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(Mode.PAYMENT)
                .addLineItem(
                        LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("eur")
                                                .setUnitAmount(amountCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Factura " + numeroFactura)
                                                                .build())
                                                .build())
                                .build())
                .setSuccessUrl(successUrl + "?factura_pago=ok&session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .putMetadata("factura_id", facturaIdMeta)
                .build();

        Session facturaCheckoutSession = Session.create(params);
        return new PaymentLinkResult(facturaCheckoutSession.getId(), facturaCheckoutSession.getUrl());
    }

    private String createStripeCustomer(Usuario usuario) throws StripeException {
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(usuario.getEmail())
                .setName(usuario.getNombre())
                .putMetadata("usuario_id", usuario.getId().toString())
                .build();

        Customer customer = Customer.create(params);
        return customer.getId();
    }
}
