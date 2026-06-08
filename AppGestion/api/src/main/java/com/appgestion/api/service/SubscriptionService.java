package com.appgestion.api.service;

import com.appgestion.api.domain.entity.StripeInvoiceLedger;
import com.appgestion.api.domain.entity.StripeSubscription;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.SubscriptionStatus;
import com.appgestion.api.dto.response.SubscriptionDetailsDto;
import com.appgestion.api.repository.StripeInvoiceLedgerRepository;
import com.appgestion.api.repository.StripeSubscriptionRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.service.stripe.StripeSubscriptionFetcher;
import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import com.stripe.model.Price;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.SubscriptionItemCollection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final UsuarioRepository usuarioRepository;
    private final StripeSubscriptionRepository stripeSubscriptionRepository;
    private final StripeInvoiceLedgerRepository stripeInvoiceLedgerRepository;
    private final StripeSubscriptionFetcher stripeSubscriptionFetcher;

    @Value("${stripe.price-id-monthly}")
    private String configuredMonthlyPriceId;

    @Value("${stripe.price-id-yearly:}")
    private String configuredYearlyPriceId;

    @Value("${app.subscription.display.monthly-eur:9.99}")
    private BigDecimal displayMonthlyEur;

    @Value("${app.subscription.display.yearly-eur:90.00}")
    private BigDecimal displayYearlyEur;

    public SubscriptionService(
            UsuarioRepository usuarioRepository,
            StripeSubscriptionRepository stripeSubscriptionRepository,
            StripeInvoiceLedgerRepository stripeInvoiceLedgerRepository,
            StripeSubscriptionFetcher stripeSubscriptionFetcher) {
        this.usuarioRepository = usuarioRepository;
        this.stripeSubscriptionRepository = stripeSubscriptionRepository;
        this.stripeInvoiceLedgerRepository = stripeInvoiceLedgerRepository;
        this.stripeSubscriptionFetcher = stripeSubscriptionFetcher;
    }

    /**
     * Expira los trials vencidos. Invocado por TrialExpirationJob.
     */
    @Transactional
    public void expireTrials() {
        LocalDate today = LocalDate.now();
        List<Usuario> expired = usuarioRepository.findExpiredTrials(SubscriptionStatus.TRIAL_ACTIVE, today);
        for (Usuario u : expired) {
            u.setSubscriptionStatus(SubscriptionStatus.TRIAL_EXPIRED);
            usuarioRepository.save(u);
        }
        if (!expired.isEmpty()) {
            log.info("SubscriptionService: expirados {} trials", expired.size());
        }
    }

    /**
     * Verifica si el usuario puede escribir (crear, editar, eliminar).
     * Trial de app, suscripción activa o trial de Stripe permiten escritura.
     */
    public boolean canWrite(Usuario usuario) {
        if (usuario == null) {
            return false;
        }
        SubscriptionStatus status = usuario.getSubscriptionStatus();
        if (status == null) {
            return false;
        }
        return status == SubscriptionStatus.TRIAL_ACTIVE
                || status == SubscriptionStatus.ACTIVE
                || status == SubscriptionStatus.TRIALING;
    }

    /**
     * Si el trial ha expirado, actualiza el estado a TRIAL_EXPIRED.
     * Debe llamarse en login para mantener consistencia.
     */
    @Transactional
    public void checkAndUpdateTrialStatus(Usuario usuario) {
        if (usuario == null) {
            return;
        }
        if (usuario.getSubscriptionStatus() != SubscriptionStatus.TRIAL_ACTIVE) {
            return;
        }
        LocalDate trialEnd = usuario.getTrialEndDate();
        if (trialEnd == null) {
            return;
        }
        if (LocalDate.now().isAfter(trialEnd)) {
            usuario.setSubscriptionStatus(SubscriptionStatus.TRIAL_EXPIRED);
            usuarioRepository.save(usuario);
        }
    }

    /**
     * Compatibilidad: equivalente a canWrite para el filtro de suscripción.
     */
    public boolean hasActiveSubscription(Usuario usuario) {
        return canWrite(usuario);
    }

    /**
     * Para desarrollo: marca al usuario como ACTIVE (premium) sin Stripe.
     */
    @Transactional
    public void grantPremiumForDev(Usuario usuario) {
        if (usuario == null) {
            return;
        }
        usuario.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        usuario.setStripeCustomerId(null);
        usuario.setStripeSubscriptionId(null);
        usuario.setSubscriptionCurrentPeriodEnd(null);
        usuario.setStripePriceId(null);
        usuario.setSubscriptionCancelAtPeriodEnd(false);
        usuario.setSubscriptionRequiresPaymentAction(false);
        usuarioRepository.save(usuario);
        log.info("Dev: usuario {} marcado como ACTIVE (premium)", usuario.getEmail());
    }

    public SubscriptionDetailsDto getSubscriptionDetails(Usuario usuario) {
        String statusName = usuario.getSubscriptionStatus() != null ? usuario.getSubscriptionStatus().name() : null;
        String interval = resolveBillingInterval(usuario.getStripePriceId());
        int savings = computeYearlySavingsPercent(displayMonthlyEur, displayYearlyEur);
        return new SubscriptionDetailsDto(
                statusName,
                usuario.getStripePriceId(),
                interval,
                usuario.isSubscriptionCancelAtPeriodEnd(),
                usuario.isSubscriptionRequiresPaymentAction(),
                usuario.getSubscriptionCurrentPeriodEnd(),
                displayMonthlyEur,
                displayYearlyEur,
                savings);
    }

    /**
     * Sincroniza suscripción Stripe → usuario + fila {@code stripe_subscriptions} (idempotente).
     */
    @Transactional
    public void syncFromStripeSubscription(Subscription subscription, Long usuarioId, String stripeCustomerIdOverride) {
        if (subscription == null) {
            return;
        }
        Usuario usuario = resolveUsuarioForSubscription(subscription, usuarioId);
        if (usuario == null) {
            log.warn("SubscriptionService: sin usuario para suscripción {}", subscription.getId());
            return;
        }
        String customerId = stripeCustomerIdOverride != null && !stripeCustomerIdOverride.isBlank()
                ? stripeCustomerIdOverride
                : subscription.getCustomer();
        if (customerId != null && !customerId.isBlank()) {
            usuario.setStripeCustomerId(customerId);
        }
        usuario.setStripeSubscriptionId(subscription.getId());
        String priceId = extractPriceId(subscription);
        usuario.setStripePriceId(priceId);
        usuario.setSubscriptionCancelAtPeriodEnd(Boolean.TRUE.equals(subscription.getCancelAtPeriodEnd()));
        usuario.setSubscriptionStatus(mapStripeStatusToEnum(subscription.getStatus()));
        usuario.setSubscriptionCurrentPeriodEnd(toLocalDateTime(subscription.getCurrentPeriodEnd()));
        if (usuario.getSubscriptionStatus() == SubscriptionStatus.ACTIVE
                || usuario.getSubscriptionStatus() == SubscriptionStatus.TRIALING) {
            usuario.setSubscriptionRequiresPaymentAction(false);
        }
        usuarioRepository.save(usuario);

        upsertStripeSubscriptionRow(usuario, subscription, priceId);
    }

    @Transactional
    public void activateSubscription(Long usuarioId, String stripeCustomerId, String stripeSubscriptionId,
            String stripeStatus, Instant currentPeriodEnd) {
        if (stripeSubscriptionId == null || stripeSubscriptionId.isBlank()) {
            return;
        }
        try {
            Subscription sub = stripeSubscriptionFetcher.fetch(stripeSubscriptionId);
            syncFromStripeSubscription(sub, usuarioId, stripeCustomerId);
        } catch (StripeException e) {
            log.warn("activateSubscription: retrieve falló {}, aplicando snapshot mínimo", e.getMessage());
            Usuario usuario = usuarioRepository.findById(Objects.requireNonNull(usuarioId))
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + usuarioId));
            usuario.setStripeCustomerId(stripeCustomerId);
            usuario.setStripeSubscriptionId(stripeSubscriptionId);
            usuario.setSubscriptionStatus(mapStripeStatusToEnum(stripeStatus));
            usuario.setSubscriptionCurrentPeriodEnd(toLocalDateTime(currentPeriodEnd));
            usuarioRepository.save(usuario);
        }
    }

    @Transactional
    public void updateSubscription(String stripeSubscriptionId, String stripeStatus, Instant currentPeriodEnd) {
        if (stripeSubscriptionId == null || stripeSubscriptionId.isBlank()) {
            return;
        }
        try {
            Subscription sub = stripeSubscriptionFetcher.fetch(stripeSubscriptionId);
            syncFromStripeSubscription(sub, null, null);
        } catch (StripeException e) {
            log.warn("updateSubscription: retrieve falló para {}: {}", stripeSubscriptionId, e.getMessage());
            Usuario usuario = usuarioRepository.findByStripeSubscriptionId(stripeSubscriptionId).orElse(null);
            if (usuario != null) {
                usuario.setSubscriptionStatus(mapStripeStatusToEnum(stripeStatus));
                if (currentPeriodEnd != null) {
                    usuario.setSubscriptionCurrentPeriodEnd(toLocalDateTime(currentPeriodEnd));
                }
                usuarioRepository.save(usuario);
            }
        }
    }

    @Transactional
    public void cancelSubscription(String stripeSubscriptionId) {
        if (stripeSubscriptionId == null || stripeSubscriptionId.isBlank()) {
            return;
        }
        stripeSubscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId).ifPresent(row -> {
            row.setStripeStatus("canceled");
            row.setCancelAtPeriodEnd(false);
            row.setUpdatedAt(LocalDateTime.now());
            stripeSubscriptionRepository.save(row);
        });
        Usuario usuario = usuarioRepository.findByStripeSubscriptionId(stripeSubscriptionId).orElse(null);
        if (usuario != null) {
            usuario.setSubscriptionStatus(SubscriptionStatus.CANCELED);
            usuario.setStripeSubscriptionId(null);
            usuario.setStripePriceId(null);
            usuario.setSubscriptionCancelAtPeriodEnd(false);
            usuario.setSubscriptionRequiresPaymentAction(false);
            usuario.setSubscriptionCurrentPeriodEnd(null);
            usuarioRepository.save(usuario);
        }
    }

    @Transactional
    public void recordInvoicePaid(Invoice invoice) {
        if (invoice == null) {
            return;
        }
        String subId = invoice.getSubscription();
        if (subId == null || subId.isBlank()) {
            return;
        }
        Usuario usuario = usuarioRepository.findByStripeSubscriptionId(subId).orElse(null);
        if (usuario == null && invoice.getCustomer() != null && !invoice.getCustomer().isBlank()) {
            usuario = usuarioRepository.findByStripeCustomerId(invoice.getCustomer()).orElse(null);
        }
        if (usuario == null) {
            return;
        }
        String currentSubscriptionId = usuario.getStripeSubscriptionId();
        boolean hasCurrentSubscription = currentSubscriptionId != null && !currentSubscriptionId.isBlank();
        boolean invoiceMatchesCurrentSubscription = subId.equals(currentSubscriptionId);
        if (hasCurrentSubscription && !invoiceMatchesCurrentSubscription) {
            recordInvoiceLedger(invoice, usuario, subId);
            log.warn("SubscriptionService: invoice {} pertenece a suscripción {} pero usuario {} usa {}; no se sincroniza",
                    invoice.getId(), subId, usuario.getEmail(), currentSubscriptionId);
            return;
        }
        if (!hasCurrentSubscription) {
            usuario.setStripeSubscriptionId(subId);
        }
        recordInvoiceLedger(invoice, usuario, subId);
        usuario.setSubscriptionRequiresPaymentAction(false);
        usuarioRepository.save(usuario);
        try {
            Subscription sub = stripeSubscriptionFetcher.fetch(subId);
            syncFromStripeSubscription(sub, null, null);
        } catch (StripeException e) {
            log.warn("recordInvoicePaid: no se pudo cargar suscripción {} tras invoice.paid: {}", subId, e.getMessage());
        }
    }

    @Transactional
    public void markRequiresPaymentAction(String stripeSubscriptionId) {
        if (stripeSubscriptionId == null || stripeSubscriptionId.isBlank()) {
            return;
        }
        Usuario u = usuarioRepository.findByStripeSubscriptionId(stripeSubscriptionId).orElse(null);
        if (u == null) {
            return;
        }
        u.setSubscriptionRequiresPaymentAction(true);
        if (u.getSubscriptionStatus() != SubscriptionStatus.TRIAL_ACTIVE) {
            u.setSubscriptionStatus(SubscriptionStatus.INCOMPLETE);
        }
        usuarioRepository.save(u);
    }

    private Usuario resolveUsuarioForSubscription(Subscription subscription, Long usuarioId) {
        if (usuarioId != null) {
            return usuarioRepository.findById(usuarioId).orElse(null);
        }
        Optional<Usuario> bySub = usuarioRepository.findByStripeSubscriptionId(subscription.getId());
        if (bySub.isPresent()) {
            return bySub.get();
        }
        String metaUid = subscription.getMetadata() != null ? subscription.getMetadata().get("usuario_id") : null;
        if (metaUid != null && !metaUid.isBlank()) {
            try {
                return usuarioRepository.findById(Long.valueOf(metaUid)).orElse(null);
            } catch (NumberFormatException ignored) {
                // ignore
            }
        }
        Optional<StripeSubscription> row = stripeSubscriptionRepository.findByStripeSubscriptionId(subscription.getId());
        return row.map(StripeSubscription::getUsuario).orElse(null);
    }

    private void recordInvoiceLedger(Invoice invoice, Usuario usuario, String subId) {
        String invoiceId = invoice.getId();
        if (invoiceId == null || invoiceId.isBlank()) {
            return;
        }
        StripeInvoiceLedger row = stripeInvoiceLedgerRepository
                .findByStripeInvoiceId(invoiceId)
                .orElseGet(StripeInvoiceLedger::new);
        row.setUsuario(usuario);
        row.setStripeSubscriptionId(subId);
        row.setStripeInvoiceId(invoiceId);
        Long paid = invoice.getAmountPaid();
        row.setAmountPaid(paid == null ? 0L : paid);
        row.setCurrency(invoice.getCurrency() != null ? invoice.getCurrency() : "eur");
        row.setStatus(invoice.getStatus());
        Long created = invoice.getCreated();
        row.setPaidAt(created != null
                ? LocalDateTime.ofInstant(Instant.ofEpochSecond(created), ZoneId.systemDefault())
                : LocalDateTime.now());
        stripeInvoiceLedgerRepository.save(row);
    }

    private void upsertStripeSubscriptionRow(Usuario usuario, Subscription subscription, String priceId) {
        StripeSubscription row = stripeSubscriptionRepository
                .findByStripeSubscriptionId(subscription.getId())
                .orElseGet(StripeSubscription::new);
        if (row.getId() == null) {
            row.setUsuario(usuario);
            row.setStripeSubscriptionId(subscription.getId());
            row.setCreatedAt(LocalDateTime.now());
        }
        row.setStripePriceId(priceId);
        row.setStripeStatus(subscription.getStatus());
        row.setCancelAtPeriodEnd(Boolean.TRUE.equals(subscription.getCancelAtPeriodEnd()));
        row.setCurrentPeriodStart(toLocalDateTime(subscription.getCurrentPeriodStart()));
        row.setCurrentPeriodEnd(toLocalDateTime(subscription.getCurrentPeriodEnd()));
        row.setTrialEnd(toLocalDateTime(subscription.getTrialEnd()));
        row.setUpdatedAt(LocalDateTime.now());
        stripeSubscriptionRepository.save(row);
    }

    private static String extractPriceId(Subscription subscription) {
        SubscriptionItemCollection items = subscription.getItems();
        if (items == null || items.getData() == null || items.getData().isEmpty()) {
            return null;
        }
        SubscriptionItem item = items.getData().get(0);
        Price price = item.getPrice();
        return price != null ? price.getId() : null;
    }

    private String resolveBillingInterval(String priceId) {
        if (priceId == null || priceId.isBlank()) {
            return "UNKNOWN";
        }
        if (configuredYearlyPriceId != null && priceId.equals(configuredYearlyPriceId)) {
            return "YEARLY";
        }
        if (priceId.equals(configuredMonthlyPriceId)) {
            return "MONTHLY";
        }
        return "UNKNOWN";
    }

    private static int computeYearlySavingsPercent(BigDecimal monthly, BigDecimal yearly) {
        if (monthly == null || yearly == null || monthly.signum() <= 0) {
            return 0;
        }
        BigDecimal annualEq = monthly.multiply(BigDecimal.valueOf(12));
        if (annualEq.signum() <= 0) {
            return 0;
        }
        BigDecimal ratio = yearly.divide(annualEq, 4, RoundingMode.HALF_UP);
        BigDecimal savings = BigDecimal.ONE.subtract(ratio);
        return savings.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private static SubscriptionStatus mapStripeStatusToEnum(String stripeStatus) {
        if (stripeStatus == null || stripeStatus.isBlank()) {
            return SubscriptionStatus.INCOMPLETE;
        }
        return switch (stripeStatus.toLowerCase()) {
            case "active" -> SubscriptionStatus.ACTIVE;
            case "trialing" -> SubscriptionStatus.TRIALING;
            case "past_due" -> SubscriptionStatus.PAST_DUE;
            case "unpaid" -> SubscriptionStatus.UNPAID;
            case "incomplete" -> SubscriptionStatus.INCOMPLETE;
            case "incomplete_expired" -> SubscriptionStatus.CANCELED;
            case "canceled", "cancelled" -> SubscriptionStatus.CANCELED;
            default -> SubscriptionStatus.CANCELED;
        };
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private static LocalDateTime toLocalDateTime(Long epochSeconds) {
        if (epochSeconds == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault());
    }
}
