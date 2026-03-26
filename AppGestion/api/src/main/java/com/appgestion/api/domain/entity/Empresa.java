package com.appgestion.api.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "empresas")
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(nullable = false, length = 200)
    private String nombre = "";

    @Column(length = 255)
    private String direccion;

    @Column(name = "codigo_postal", length = 10)
    private String codigoPostal;

    @Column(length = 100)
    private String provincia;

    @Column(length = 100)
    private String pais = "España";

    @Column(length = 20)
    private String nif;

    @Column(length = 50)
    private String telefono;

    @Column(length = 150)
    private String email;

    @Column(name = "notas_pie_presupuesto", length = 1000)
    private String notasPiePresupuesto;

    @Column(name = "notas_pie_factura", length = 1000)
    private String notasPieFactura;

    @Column(name = "mail_host", length = 100)
    private String mailHost;

    @Column(name = "mail_port")
    private Integer mailPort;

    @Column(name = "mail_username", length = 150)
    private String mailUsername;

    @Column(name = "mail_password", length = 255)
    private String mailPassword;

    /**
     * Imagen de firma para PDFs (PNG/JPEG).
     * No usar {@code @Lob}: en PostgreSQL Hibernate lo mapea a {@code oid}, pero la columna es {@code BYTEA}.
     */
    @Column(name = "firma_imagen", columnDefinition = "BYTEA")
    private byte[] firmaImagen;

    /** Logo para cabecera de presupuestos y facturas (PNG/JPEG). */
    @Column(name = "logo_imagen", columnDefinition = "BYTEA")
    private byte[] logoImagen;

    /** Valor por defecto del campo "Método de pago" en nuevas facturas. */
    @Column(name = "default_metodo_pago", length = 50)
    private String defaultMetodoPago;

    @Column(name = "default_condiciones_pago", length = 200)
    private String defaultCondicionesPago;

    @Column(name = "iban_cuenta", length = 34)
    private String ibanCuenta;

    @Column(name = "bizum_telefono", length = 20)
    private String bizumTelefono;

    /** Régimen de IVA/IGIC principal (texto mostrado en factura). */
    @Column(name = "regimen_iva_principal", length = 120)
    private String regimenIvaPrincipal;

    @Column(name = "descripcion_actividad_fiscal", length = 500)
    private String descripcionActividadFiscal;

    @Column(name = "nif_intracomunitario", length = 20)
    private String nifIntracomunitario;

    @Column(name = "epigrafe_iae", length = 30)
    private String epigrafeIae;

    /** Código de rubro/actividad (autónomo); solo métricas internas, no se imprime en PDF. */
    @Column(name = "rubro_autonomo_codigo", length = 64)
    private String rubroAutonomoCodigo;

    /** Si true, se envían recordatorios de pago por email al cliente (facturas impagadas). */
    @Column(name = "recordatorio_cliente_activo", nullable = false)
    private Boolean recordatorioClienteActivo = false;

    /** Días tras la fecha de vencimiento en los que enviar (ej. "7,15,30"). */
    @Column(name = "recordatorio_cliente_dias", nullable = false, length = 32)
    private String recordatorioClienteDias = "7,15,30";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre != null ? nombre : ""; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getCodigoPostal() { return codigoPostal; }
    public void setCodigoPostal(String codigoPostal) { this.codigoPostal = codigoPostal; }

    public String getProvincia() { return provincia; }
    public void setProvincia(String provincia) { this.provincia = provincia; }

    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais != null ? pais : "España"; }

    public String getNif() { return nif; }
    public void setNif(String nif) { this.nif = nif; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNotasPiePresupuesto() { return notasPiePresupuesto; }
    public void setNotasPiePresupuesto(String notasPiePresupuesto) { this.notasPiePresupuesto = notasPiePresupuesto; }

    public String getNotasPieFactura() { return notasPieFactura; }
    public void setNotasPieFactura(String notasPieFactura) { this.notasPieFactura = notasPieFactura; }

    public String getMailHost() { return mailHost; }
    public void setMailHost(String mailHost) { this.mailHost = mailHost; }

    public Integer getMailPort() { return mailPort; }
    public void setMailPort(Integer mailPort) { this.mailPort = mailPort; }

    public String getMailUsername() { return mailUsername; }
    public void setMailUsername(String mailUsername) { this.mailUsername = mailUsername; }

    public String getMailPassword() { return mailPassword; }
    public void setMailPassword(String mailPassword) { this.mailPassword = mailPassword; }

    public byte[] getFirmaImagen() { return firmaImagen; }
    public void setFirmaImagen(byte[] firmaImagen) { this.firmaImagen = firmaImagen; }

    public byte[] getLogoImagen() { return logoImagen; }
    public void setLogoImagen(byte[] logoImagen) { this.logoImagen = logoImagen; }

    public String getDefaultMetodoPago() { return defaultMetodoPago; }
    public void setDefaultMetodoPago(String defaultMetodoPago) { this.defaultMetodoPago = defaultMetodoPago; }

    public String getDefaultCondicionesPago() { return defaultCondicionesPago; }
    public void setDefaultCondicionesPago(String defaultCondicionesPago) { this.defaultCondicionesPago = defaultCondicionesPago; }

    public String getIbanCuenta() { return ibanCuenta; }
    public void setIbanCuenta(String ibanCuenta) { this.ibanCuenta = ibanCuenta; }

    public String getBizumTelefono() { return bizumTelefono; }
    public void setBizumTelefono(String bizumTelefono) { this.bizumTelefono = bizumTelefono; }

    public String getRegimenIvaPrincipal() { return regimenIvaPrincipal; }
    public void setRegimenIvaPrincipal(String regimenIvaPrincipal) { this.regimenIvaPrincipal = regimenIvaPrincipal; }

    public String getDescripcionActividadFiscal() { return descripcionActividadFiscal; }
    public void setDescripcionActividadFiscal(String descripcionActividadFiscal) { this.descripcionActividadFiscal = descripcionActividadFiscal; }

    public String getNifIntracomunitario() { return nifIntracomunitario; }
    public void setNifIntracomunitario(String nifIntracomunitario) { this.nifIntracomunitario = nifIntracomunitario; }

    public String getEpigrafeIae() { return epigrafeIae; }
    public void setEpigrafeIae(String epigrafeIae) { this.epigrafeIae = epigrafeIae; }

    public String getRubroAutonomoCodigo() { return rubroAutonomoCodigo; }
    public void setRubroAutonomoCodigo(String rubroAutonomoCodigo) { this.rubroAutonomoCodigo = rubroAutonomoCodigo; }

    public Boolean getRecordatorioClienteActivo() { return recordatorioClienteActivo; }
    public void setRecordatorioClienteActivo(Boolean recordatorioClienteActivo) {
        this.recordatorioClienteActivo = recordatorioClienteActivo != null ? recordatorioClienteActivo : false;
    }

    public String getRecordatorioClienteDias() { return recordatorioClienteDias; }
    public void setRecordatorioClienteDias(String recordatorioClienteDias) {
        this.recordatorioClienteDias = recordatorioClienteDias != null ? recordatorioClienteDias : "7,15,30";
    }
}
