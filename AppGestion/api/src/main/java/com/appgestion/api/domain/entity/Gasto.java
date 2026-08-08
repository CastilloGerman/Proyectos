package com.appgestion.api.domain.entity;

import com.appgestion.api.domain.enums.GastoCategoria;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "gastos")
public class Gasto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 200)
    private String proveedor;

    @Column(nullable = false, length = 500)
    private String concepto;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "base_imponible", nullable = false)
    private Double baseImponible = 0.0;

    @Column(name = "tipo_iva", nullable = false)
    private Double tipoIva = 21.0;

    @Column(name = "cuota_iva", nullable = false)
    private Double cuotaIva = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GastoCategoria categoria = GastoCategoria.OTROS;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }

    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public Double getBaseImponible() { return baseImponible; }
    public void setBaseImponible(Double baseImponible) { this.baseImponible = baseImponible; }

    public Double getTipoIva() { return tipoIva; }
    public void setTipoIva(Double tipoIva) { this.tipoIva = tipoIva; }

    public Double getCuotaIva() { return cuotaIva; }
    public void setCuotaIva(Double cuotaIva) { this.cuotaIva = cuotaIva; }

    public GastoCategoria getCategoria() { return categoria; }
    public void setCategoria(GastoCategoria categoria) { this.categoria = categoria; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
