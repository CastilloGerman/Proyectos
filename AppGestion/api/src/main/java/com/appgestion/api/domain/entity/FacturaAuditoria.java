package com.appgestion.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * SEC-15: registro inmutable de un cambio en una factura. {@code hash} encadena con {@code prevHash} (registro
 * anterior de la misma factura). Ver {@link com.appgestion.api.service.FacturaAuditoriaService}.
 */
@Entity
@Table(name = "factura_auditoria", indexes = {
        @Index(name = "idx_factura_auditoria_factura", columnList = "factura_id, id"),
        @Index(name = "idx_factura_auditoria_usuario", columnList = "usuario_id")
})
public class FacturaAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "factura_id", nullable = false)
    private Long facturaId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 30)
    private String evento;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "datos_json", nullable = false, columnDefinition = "TEXT")
    private String datosJson;

    @Column(name = "prev_hash", nullable = false, length = 64)
    private String prevHash;

    @Column(nullable = false, length = 64)
    private String hash;

    public Long getId() { return id; }
    public Long getFacturaId() { return facturaId; }
    public void setFacturaId(Long facturaId) { this.facturaId = facturaId; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getEvento() { return evento; }
    public void setEvento(String evento) { this.evento = evento; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getDatosJson() { return datosJson; }
    public void setDatosJson(String datosJson) { this.datosJson = datosJson; }
    public String getPrevHash() { return prevHash; }
    public void setPrevHash(String prevHash) { this.prevHash = prevHash; }
    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
}
