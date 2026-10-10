package com.appgestion.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Registra migraciones de aplicación ya ejecutadas para evitar re-ejecutarlas en cada arranque.
 */
@Entity
@Table(name = "app_migration_state")
public class AppMigrationState {

    @Id
    @Column(name = "\"key\"", length = 64)
    private String key;

    @Column(name = "\"value\"", nullable = false)
    private String value;

    @Column(name = "executed_at", nullable = false)
    private OffsetDateTime executedAt;

    public AppMigrationState() {}

    public AppMigrationState(String key, String value) {
        this.key = key;
        this.value = value;
        this.executedAt = OffsetDateTime.now();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public OffsetDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(OffsetDateTime executedAt) {
        this.executedAt = executedAt;
    }
}
