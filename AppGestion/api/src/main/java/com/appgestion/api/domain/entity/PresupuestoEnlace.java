package com.appgestion.api.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.Instant;

@Entity
@Table(name = "presupuesto_enlace")
public class PresupuestoEnlace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "presupuesto_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Presupuesto presupuesto;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "creado_at", nullable = false)
    private Instant creadoAt;

    @Column(name = "expira_at", nullable = false)
    private Instant expiraAt;

    @Column(nullable = false)
    private boolean revocado;

    @Column(name = "primera_vista_at")
    private Instant primeraVistaAt;

    @Column(name = "ultima_vista_at")
    private Instant ultimaVistaAt;

    @Column(name = "num_vistas", nullable = false)
    private long numVistas;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Presupuesto getPresupuesto() { return presupuesto; }
    public void setPresupuesto(Presupuesto presupuesto) { this.presupuesto = presupuesto; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public Instant getCreadoAt() { return creadoAt; }
    public void setCreadoAt(Instant creadoAt) { this.creadoAt = creadoAt; }
    public Instant getExpiraAt() { return expiraAt; }
    public void setExpiraAt(Instant expiraAt) { this.expiraAt = expiraAt; }
    public boolean isRevocado() { return revocado; }
    public void setRevocado(boolean revocado) { this.revocado = revocado; }
    public Instant getPrimeraVistaAt() { return primeraVistaAt; }
    public void setPrimeraVistaAt(Instant primeraVistaAt) { this.primeraVistaAt = primeraVistaAt; }
    public Instant getUltimaVistaAt() { return ultimaVistaAt; }
    public void setUltimaVistaAt(Instant ultimaVistaAt) { this.ultimaVistaAt = ultimaVistaAt; }
    public long getNumVistas() { return numVistas; }
    public void setNumVistas(long numVistas) { this.numVistas = numVistas; }
}
