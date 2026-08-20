package com.appgestion.api.repository;

import com.appgestion.api.domain.entity.ProcessedStripeEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedStripeEventRepository extends JpaRepository<ProcessedStripeEvent, Long> {

    boolean existsByEventId(String eventId);
}
