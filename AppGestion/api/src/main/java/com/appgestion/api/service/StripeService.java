package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.repository.UsuarioRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionCreateParams.LineItem;
import com.stripe.param.checkout.SessionCreateParams.Mode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class StripeService {

    private final UsuarioRepository usuarioRepository;

    public StripeService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Value("${stripe.price-id-monthly}")
    private String priceIdMonthly;

    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    @Value("${stripe.portal-return-url}")
    private String portalReturnUrl;

    public String createCheckoutSession(Usuario usuario) throws StripeException {
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
                                .setPrice(priceIdMonthly)
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

    private String createStripeCustomer(Usuario usuario) throws StripeException {
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(usuario.getEmail())
                .setName(usuario.getNombre())
                .build();

        Customer customer = Customer.create(params);
        return customer.getId();
    }
}
