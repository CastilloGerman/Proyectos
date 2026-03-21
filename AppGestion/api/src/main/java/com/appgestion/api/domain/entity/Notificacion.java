package com.appgestion.api.domain.entity;

import com.appgestion.api.domain.enums.NotificacionSeveridad;
import com.appgestion.api.domain.enums.NotificacionTipo;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificacionTipo tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificacionSeveridad severidad = NotificacionSeveridad.INFO;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 1000)
    private String resumen;

    @Column(nullable = false)
    private boolean leida = false;

    @Column(name = "action_path", length = 500)
    private String actionPath;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public NotificacionTipo getTipo() {
        return tipo;
    }

    public void setTipo(NotificacionTipo tipo) {
        this.tipo = tipo;
    }

    public NotificacionSeveridad getSeveridad() {
        return severidad;
    }

    public void setSeveridad(NotificacionSeveridad severidad) {
        this.severidad = severidad;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getResumen() {
        return resumen;
    }

    public void setResumen(String resumen) {
        this.resumen = resumen;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }

    public String getActionPath() {
        return actionPath;
    }

    public void setActionPath(String actionPath) {
        this.actionPath = actionPath;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
