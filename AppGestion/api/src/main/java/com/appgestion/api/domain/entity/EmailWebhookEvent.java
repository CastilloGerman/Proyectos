package com.appgestion.api.domain.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "email_webhook_events")
public class EmailWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String provider;

    @Column(name = "event_type", length = 64)
    private String eventType;

    @Column(name = "external_id", length = 255)
    private String externalId;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }
}
