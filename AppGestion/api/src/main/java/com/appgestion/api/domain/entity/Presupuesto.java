package com.appgestion.api.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "presupuestos")
public class Presupuesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(nullable = false)
    private Double subtotal = 0.0;

    @Column(nullable = false)
    private Double iva = 0.0;

    @Column(nullable = false)
    private Double total = 0.0;

    @Column(name = "iva_habilitado", nullable = false)
    private Boolean ivaHabilitado = true;

    @Column(length = 50)
    private String estado = "Pendiente";

    @Column(name = "descuento_global_porcentaje")
    private Double descuentoGlobalPorcentaje = 0.0;

    @Column(name = "descuento_global_fijo")
    private Double descuentoGlobalFijo = 0.0;

    @Column(name = "descuento_antes_iva")
    private Boolean descuentoAntesIva = true;

    @OneToMany(mappedBy = "presupuesto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PresupuestoItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }

    public Double getIva() { return iva; }
    public void setIva(Double iva) { this.iva = iva; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public Boolean getIvaHabilitado() { return ivaHabilitado; }
    public void setIvaHabilitado(Boolean ivaHabilitado) { this.ivaHabilitado = ivaHabilitado; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Double getDescuentoGlobalPorcentaje() { return descuentoGlobalPorcentaje; }
    public void setDescuentoGlobalPorcentaje(Double descuentoGlobalPorcentaje) { this.descuentoGlobalPorcentaje = descuentoGlobalPorcentaje; }

    public Double getDescuentoGlobalFijo() { return descuentoGlobalFijo; }
    public void setDescuentoGlobalFijo(Double descuentoGlobalFijo) { this.descuentoGlobalFijo = descuentoGlobalFijo; }

    public Boolean getDescuentoAntesIva() { return descuentoAntesIva; }
    public void setDescuentoAntesIva(Boolean descuentoAntesIva) { this.descuentoAntesIva = descuentoAntesIva; }

    public List<PresupuestoItem> getItems() { return items; }
    public void setItems(List<PresupuestoItem> items) { this.items = items; }
}
