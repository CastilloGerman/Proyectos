package com.appgestion.api.repository;

import com.appgestion.api.domain.entity.EmailWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailWebhookEventRepository extends JpaRepository<EmailWebhookEvent, Long> {
}
