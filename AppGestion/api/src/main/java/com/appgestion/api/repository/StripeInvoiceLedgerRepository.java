package com.appgestion.api.repository;

import com.appgestion.api.domain.entity.StripeInvoiceLedger;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StripeInvoiceLedgerRepository extends JpaRepository<StripeInvoiceLedger, Long> {

    Optional<StripeInvoiceLedger> findByStripeInvoiceId(String stripeInvoiceId);
}