package com.appgestion.api.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "factura_items")
public class FacturaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id", nullable = false)
    private Factura factura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    private Material material;

    @Column(name = "tarea_manual", length = 500)
    private String tareaManual;

    @Column(nullable = false)
    private Double cantidad = 1.0;

    @Column(name = "precio_unitario", nullable = false)
    private Double precioUnitario = 0.0;

    @Column(nullable = false)
    private Double subtotal = 0.0;

    @Column(name = "cuota_iva")
    private Double cuotaIva = 0.0;

    @Column(name = "es_tarea_manual")
    private Boolean esTareaManual = false;

    @Column(name = "aplica_iva")
    private Boolean aplicaIva = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Factura getFactura() { return factura; }
    public void setFactura(Factura factura) { this.factura = factura; }

    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }

    public String getTareaManual() { return tareaManual; }
    public void setTareaManual(String tareaManual) { this.tareaManual = tareaManual; }

    public Double getCantidad() { return cantidad; }
    public void setCantidad(Double cantidad) { this.cantidad = cantidad; }

    public Double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(Double precioUnitario) { this.precioUnitario = precioUnitario; }

    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }

    public Double getCuotaIva() { return cuotaIva; }
    public void setCuotaIva(Double cuotaIva) { this.cuotaIva = cuotaIva; }

    public Boolean getEsTareaManual() { return esTareaManual; }
    public void setEsTareaManual(Boolean esTareaManual) { this.esTareaManual = esTareaManual; }

    public Boolean getAplicaIva() { return aplicaIva; }
    public void setAplicaIva(Boolean aplicaIva) { this.aplicaIva = aplicaIva; }
}
