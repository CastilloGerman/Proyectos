package com.appgestion.api.domain.entity;

import com.appgestion.api.domain.enums.TipoFactura;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "facturas", uniqueConstraints = @UniqueConstraint(columnNames = {"numero_factura", "usuario_id"}))
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "numero_factura", nullable = false, length = 50)
    private String numeroFactura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "presupuesto_id")
    private Presupuesto presupuesto;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_expedicion")
    private LocalDate fechaExpedicion;

    @Column(name = "fecha_operacion")
    private LocalDate fechaOperacion;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "regimen_fiscal", length = 100)
    private String regimenFiscal = "Régimen general del IVA";

    @Column(name = "condiciones_pago", length = 100)
    private String condicionesPago;

    @Column(length = 10)
    private String moneda = "EUR";

    @Column(nullable = false)
    private Double subtotal = 0.0;

    @Column(nullable = false)
    private Double iva = 0.0;

    @Column(nullable = false)
    private Double total = 0.0;

    @Column(name = "iva_habilitado", nullable = false)
    private Boolean ivaHabilitado = true;

    @Column(name = "metodo_pago", length = 50)
    private String metodoPago = "Transferencia";

    @Column(name = "estado_pago", length = 50)
    private String estadoPago = "No Pagada";

    @Column(name = "monto_cobrado")
    private Double montoCobrado;

    @Column(name = "recordatorio_enviado")
    private Boolean recordatorioEnviado = false;

    /** Días (7,15,30) en los que ya se envió recordatorio al cliente; separados por coma. */
    @Column(name = "recordatorio_cliente_marcas", length = 32)
    private String recordatorioClienteMarcas;

    @Column(name = "payment_link_url", columnDefinition = "TEXT")
    private String paymentLinkUrl;

    @Column(name = "payment_link_id", length = 120)
    private String paymentLinkId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_factura", length = 20, nullable = false)
    private TipoFactura tipoFactura = TipoFactura.NORMAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_anticipo_id")
    private Factura facturaAnticipoReferencia;

    @Column(name = "importe_anticipo_descontado", precision = 10, scale = 2)
    private BigDecimal importeAnticipoDescontado;

    @Column(length = 1000)
    private String notas;

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FacturaItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getNumeroFactura() { return numeroFactura; }
    public void setNumeroFactura(String numeroFactura) { this.numeroFactura = numeroFactura; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public Presupuesto getPresupuesto() { return presupuesto; }
    public void setPresupuesto(Presupuesto presupuesto) { this.presupuesto = presupuesto; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDate getFechaExpedicion() { return fechaExpedicion; }
    public void setFechaExpedicion(LocalDate fechaExpedicion) { this.fechaExpedicion = fechaExpedicion; }

    public LocalDate getFechaOperacion() { return fechaOperacion; }
    public void setFechaOperacion(LocalDate fechaOperacion) { this.fechaOperacion = fechaOperacion; }

    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }

    public Double getIva() { return iva; }
    public void setIva(Double iva) { this.iva = iva; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public Boolean getIvaHabilitado() { return ivaHabilitado; }
    public void setIvaHabilitado(Boolean ivaHabilitado) { this.ivaHabilitado = ivaHabilitado; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public String getEstadoPago() { return estadoPago; }
    public void setEstadoPago(String estadoPago) { this.estadoPago = estadoPago; }

    public Double getMontoCobrado() { return montoCobrado; }
    public void setMontoCobrado(Double montoCobrado) { this.montoCobrado = montoCobrado; }

    public Boolean getRecordatorioEnviado() { return recordatorioEnviado; }
    public void setRecordatorioEnviado(Boolean recordatorioEnviado) { this.recordatorioEnviado = recordatorioEnviado; }

    public String getRecordatorioClienteMarcas() { return recordatorioClienteMarcas; }
    public void setRecordatorioClienteMarcas(String recordatorioClienteMarcas) { this.recordatorioClienteMarcas = recordatorioClienteMarcas; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }

    public String getRegimenFiscal() { return regimenFiscal; }
    public void setRegimenFiscal(String regimenFiscal) { this.regimenFiscal = regimenFiscal != null ? regimenFiscal : "Régimen general del IVA"; }

    public String getCondicionesPago() { return condicionesPago; }
    public void setCondicionesPago(String condicionesPago) { this.condicionesPago = condicionesPago; }

    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda != null ? moneda : "EUR"; }

    public List<FacturaItem> getItems() { return items; }
    public void setItems(List<FacturaItem> items) { this.items = items; }

    public String getPaymentLinkUrl() { return paymentLinkUrl; }
    public void setPaymentLinkUrl(String paymentLinkUrl) { this.paymentLinkUrl = paymentLinkUrl; }

    public String getPaymentLinkId() { return paymentLinkId; }
    public void setPaymentLinkId(String paymentLinkId) { this.paymentLinkId = paymentLinkId; }

    public TipoFactura getTipoFactura() { return tipoFactura; }
    public void setTipoFactura(TipoFactura tipoFactura) { this.tipoFactura = tipoFactura != null ? tipoFactura : TipoFactura.NORMAL; }

    public Factura getFacturaAnticipoReferencia() { return facturaAnticipoReferencia; }
    public void setFacturaAnticipoReferencia(Factura facturaAnticipoReferencia) { this.facturaAnticipoReferencia = facturaAnticipoReferencia; }

    public BigDecimal getImporteAnticipoDescontado() { return importeAnticipoDescontado; }
    public void setImporteAnticipoDescontado(BigDecimal importeAnticipoDescontado) { this.importeAnticipoDescontado = importeAnticipoDescontado; }
}
