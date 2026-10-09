package com.appgestion.api.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.Instant;

@Entity
@Table(name = "presupuesto_seguimiento_aviso",
        uniqueConstraints = @UniqueConstraint(name = "uk_seguimiento_aviso_presupuesto_numero",
                columnNames = {"presupuesto_id", "numero_aviso"}))
public class PresupuestoSeguimientoAviso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "presupuesto_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Presupuesto presupuesto;

    @Column(nullable = false, length = 32)
    private String tipo;

    @Column(name = "numero_aviso", nullable = false)
    private int numeroAviso;

    @Column(name = "creado_at", nullable = false)
    private Instant creadoAt;

    public Long getId() { return id; }
    public Presupuesto getPresupuesto() { return presupuesto; }
    public void setPresupuesto(Presupuesto presupuesto) { this.presupuesto = presupuesto; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public int getNumeroAviso() { return numeroAviso; }
    public void setNumeroAviso(int numeroAviso) { this.numeroAviso = numeroAviso; }
    public Instant getCreadoAt() { return creadoAt; }
    public void setCreadoAt(Instant creadoAt) { this.creadoAt = creadoAt; }
}
