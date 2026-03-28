package com.appgestion.api.domain.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "oauth_pending")
public class OauthPending {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "state_token", nullable = false, unique = true, length = 64)
    private String stateToken;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 32)
    private String provider;

    @Column(name = "code_verifier", length = 128)
    private String codeVerifier;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public Long getId() {
        return id;
    }

    public String getStateToken() {
        return stateToken;
    }

    public void setStateToken(String stateToken) {
        this.stateToken = stateToken;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getCodeVerifier() {
        return codeVerifier;
    }

    public void setCodeVerifier(String codeVerifier) {
        this.codeVerifier = codeVerifier;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
