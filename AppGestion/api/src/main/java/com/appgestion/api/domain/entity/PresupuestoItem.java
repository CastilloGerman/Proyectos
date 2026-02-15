package com.appgestion.api.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "presupuesto_items")
public class PresupuestoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "presupuesto_id", nullable = false)
    private Presupuesto presupuesto;

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

    @Column(name = "visible_pdf")
    private Boolean visiblePdf = true;

    @Column(name = "es_tarea_manual")
    private Boolean esTareaManual = false;

    @Column(name = "aplica_iva")
    private Boolean aplicaIva = true;

    @Column(name = "descuento_porcentaje")
    private Double descuentoPorcentaje = 0.0;

    @Column(name = "descuento_fijo")
    private Double descuentoFijo = 0.0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Presupuesto getPresupuesto() { return presupuesto; }
    public void setPresupuesto(Presupuesto presupuesto) { this.presupuesto = presupuesto; }

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

    public Boolean getVisiblePdf() { return visiblePdf; }
    public void setVisiblePdf(Boolean visiblePdf) { this.visiblePdf = visiblePdf; }

    public Boolean getEsTareaManual() { return esTareaManual; }
    public void setEsTareaManual(Boolean esTareaManual) { this.esTareaManual = esTareaManual; }

    public Boolean getAplicaIva() { return aplicaIva; }
    public void setAplicaIva(Boolean aplicaIva) { this.aplicaIva = aplicaIva; }

    public Double getDescuentoPorcentaje() { return descuentoPorcentaje; }
    public void setDescuentoPorcentaje(Double descuentoPorcentaje) { this.descuentoPorcentaje = descuentoPorcentaje; }

    public Double getDescuentoFijo() { return descuentoFijo; }
    public void setDescuentoFijo(Double descuentoFijo) { this.descuentoFijo = descuentoFijo; }
}
