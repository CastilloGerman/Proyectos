package com.appgestion.api.repository;

import com.appgestion.api.domain.entity.StripeSubscription;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StripeSubscriptionRepository extends JpaRepository<StripeSubscription, Long> {

    Optional<StripeSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);
}
