package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.dto.SubscriptionBillingPeriod;
import com.appgestion.api.repository.UsuarioRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceCollection;
import com.stripe.model.checkout.Session;
import com.stripe.param.InvoiceListParams;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionCreateParams.LineItem;
import com.stripe.param.checkout.SessionCreateParams.Mode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.appgestion.api.dto.response.SubscriptionInvoiceDto;

@Service
public class StripeService {

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

    @Value("${stripe.portal-return-url}")
    private String portalReturnUrl;

    public String createCheckoutSession(Usuario usuario, SubscriptionBillingPeriod billingPeriod)
            throws StripeException {
        SubscriptionBillingPeriod period =
                billingPeriod != null ? billingPeriod : SubscriptionBillingPeriod.MONTHLY;
        String priceId =
                switch (period) {
                    case YEARLY -> priceIdYearly;
                    case MONTHLY -> priceIdMonthly;
                };
        if (priceId == null || priceId.isBlank()) {
            throw new IllegalStateException(
                    period == SubscriptionBillingPeriod.YEARLY
                            ? "Falta STRIPE_PRICE_YEARLY en la configuración del servidor (precio anual Stripe)."
                            : "Falta STRIPE_PRICE_MONTHLY en la configuración del servidor (precio recurrente Stripe).");
        }
        if (successUrl == null || successUrl.isBlank() || cancelUrl == null || cancelUrl.isBlank()) {
            throw new IllegalStateException(
                    "Faltan STRIPE_SUCCESS_URL y/o STRIPE_CANCEL_URL en la configuración del servidor.");
        }
        String customerId = usuario.getStripeCustomerId();
        if (customerId == null || customerId.isBlank()) {
            customerId = createStripeCustomer(usuario);
            usuario.setStripeCustomerId(customerId);
            usuarioRepository.save(usuario);
        }

        Map<String, String> metadata = new HashMap<>();
        metadata.put("usuario_id", usuario.getId().toString());

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(Mode.SUBSCRIPTION)
                .setCustomer(customerId)
                .addLineItem(
                        LineItem.builder()
                                .setPrice(priceId)
                                .setQuantity(1L)
                                .build()
                )
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .putAllMetadata(metadata)
                .build();

        Session session = Session.create(params);
        return session.getUrl();
    }

    /**
     * Facturas de suscripción asociadas al cliente Stripe (más recientes primero).
     */
    public List<SubscriptionInvoiceDto> listCustomerInvoices(String stripeCustomerId, int limit) throws StripeException {
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
            long dueCents = amountDue == null ? 0L : amountDue.longValue();
            long paidCents = amountPaid == null ? 0L : amountPaid.longValue();
            long createdUnix = created == null ? 0L : created.longValue();
            out.add(new SubscriptionInvoiceDto(
                    inv.getId(),
                    inv.getNumber(),
                    inv.getStatus() != null ? inv.getStatus() : "",
                    dueCents,
                    paidCents,
                    currency != null ? currency : "eur",
                    createdUnix,
                    inv.getInvoicePdf(),
                    inv.getHostedInvoiceUrl()
            ));
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
    public PaymentLinkResult createFacturaCheckoutUrl(String numeroFactura, long amountCents, String facturaIdMeta) throws StripeException {
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

        Session session = Session.create(params);
        return new PaymentLinkResult(session.getId(), session.getUrl());
    }

    private String createStripeCustomer(Usuario usuario) throws StripeException {
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(usuario.getEmail())
                .setName(usuario.getNombre())
                .build();

        Customer customer = Customer.create(params);
        return customer.getId();
    }
}
