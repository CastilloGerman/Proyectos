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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre != null ? nombre : ""; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

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
}
