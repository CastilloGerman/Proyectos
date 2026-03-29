package com.appgestion.api.service;

import com.appgestion.api.domain.entity.*;
import com.appgestion.api.domain.enums.TipoFactura;
import com.appgestion.api.dto.request.PlantillaPdfEscenario;
import com.appgestion.api.dto.request.PlantillaPdfTipo;
import com.appgestion.api.dto.request.PlantillasPdfPreviewRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PlantillasPdfPreviewService {

    private static final String LONG_DESC = "Instalación integral de sistema de climatización por conductos "
            + "con unidad exterior inverter, distribución en falsos techos registrables, rejillas lineales y "
            + "puesta en marcha con certificado de eficiencia. Incluye desplazamientos en radio 40 km, PPE y "
            + "retirada de residuos no peligrosos según normativa local. Validez del presupuesto 30 días.";

    private final FacturaPdfService facturaPdfService;
    private final PresupuestoPdfService presupuestoPdfService;

    public PlantillasPdfPreviewService(FacturaPdfService facturaPdfService, PresupuestoPdfService presupuestoPdfService) {
        this.facturaPdfService = facturaPdfService;
        this.presupuestoPdfService = presupuestoPdfService;
    }

    public byte[] generar(Long usuarioId, PlantillasPdfPreviewRequest request) {
        PlantillaPdfEscenario escenario = request.escenario() != null ? request.escenario() : PlantillaPdfEscenario.DEFAULT;
        String notasPie = request.notasPie();

        if (request.tipo() == PlantillaPdfTipo.FACTURA) {
            Factura factura = buildFactura(escenario);
            return facturaPdfService.generarVistaPrevia(factura, usuarioId, notasPie);
        }
        Presupuesto presupuesto = buildPresupuesto(escenario);
        return presupuestoPdfService.generarVistaPrevia(presupuesto, usuarioId, notasPie);
    }

    private static Cliente clienteEjemplo() {
        Cliente c = new Cliente();
        c.setId(-1L);
        c.setNombre("Cliente de ejemplo S.L.");
        c.setDni("B12345678");
        c.setDireccion("Calle Falsa 123");
        c.setCodigoPostal("28001");
        c.setProvincia("Madrid");
        c.setPais("España");
        c.setEmail("facturacion@cliente-ejemplo.test");
        c.setTelefono("+34 912 345 678");
        return c;
    }

    private Factura buildFactura(PlantillaPdfEscenario escenario) {
        Factura f = new Factura();
        f.setId(-1L);
        f.setNumeroFactura("FAC-PREV-2025-0001");
        f.setCliente(clienteEjemplo());
        f.setFechaExpedicion(LocalDate.of(2025, 3, 22));
        f.setFechaOperacion(LocalDate.of(2025, 3, 20));
        f.setFechaVencimiento(LocalDate.of(2025, 4, 21));
        f.setMoneda("EUR");
        f.setIvaHabilitado(true);
        f.setMetodoPago("Transferencia");
        f.setEstadoPago("No Pagada");
        f.setTipoFactura(TipoFactura.NORMAL);
        f.setRegimenFiscal("Régimen general del IVA");
        f.setCondicionesPago("Transferencia a 30 días");

        List<FacturaItem> items = new ArrayList<>();
        switch (escenario) {
            case MIXED_IVA -> {
                f.setSubtotal(180.0);
                f.setIva(27.3);
                f.setTotal(207.3);
                items.add(manualFacturaLine(f, "Servicio técnico (IVA general)", 1.0, 100.0, 100.0, true));
                items.add(manualFacturaLine(f, "Portes y embalaje (exento)", 1.0, 50.0, 50.0, false));
                items.add(manualFacturaLine(f, "Material fungible", 2.0, 15.0, 30.0, true));
            }
            case LONG_LINES -> {
                f.setSubtotal(770.0);
                f.setIva(161.7);
                f.setTotal(931.7);
                items.add(manualFacturaLine(f, LONG_DESC, 10.0, 65.0, 650.0, true));
                items.add(manualFacturaLine(f, "Mantenimiento mensual — revisión de filtros, comprobación de presiones "
                        + "y registro fotográfico en panel de control.", 1.0, 120.0, 120.0, true));
            }
            case DEFAULT, LONG_FOOTER -> {
                f.setSubtotal(770.0);
                f.setIva(161.7);
                f.setTotal(931.7);
                items.add(manualFacturaLine(f, "Servicio de consultoría", 10.0, 65.0, 650.0, true));
                items.add(manualFacturaLine(f, "Mantenimiento mensual", 1.0, 120.0, 120.0, true));
            }
        }
        f.setItems(items);
        return f;
    }

    private static FacturaItem manualFacturaLine(
            Factura factura, String descripcion, double cantidad, double pu, double subtotal, boolean aplicaIva) {
        FacturaItem it = new FacturaItem();
        it.setFactura(factura);
        it.setEsTareaManual(true);
        it.setTareaManual(descripcion);
        it.setCantidad(cantidad);
        it.setPrecioUnitario(pu);
        it.setSubtotal(subtotal);
        it.setAplicaIva(aplicaIva);
        return it;
    }

    private Presupuesto buildPresupuesto(PlantillaPdfEscenario escenario) {
        Presupuesto p = new Presupuesto();
        p.setId(-1L);
        p.setCliente(clienteEjemplo());
        p.setFechaCreacion(LocalDateTime.of(2025, 3, 22, 10, 30));
        p.setEstado("Pendiente");
        p.setIvaHabilitado(true);
        p.setCondicionesActivasJson(null);
        p.setNotaAdicional(null);

        List<PresupuestoItem> items = new ArrayList<>();
        switch (escenario) {
            case MIXED_IVA -> {
                p.setSubtotal(180.0);
                p.setIva(27.3);
                p.setTotal(207.3);
                items.add(manualPresuLine(p, "Servicio técnico (IVA general)", 1.0, 100.0, 100.0));
                items.add(manualPresuLine(p, "Portes (sin IVA en PDF factura — aquí línea compacta)", 1.0, 50.0, 50.0));
                items.add(manualPresuLine(p, "Material fungible", 2.0, 15.0, 30.0));
            }
            case LONG_LINES -> {
                p.setSubtotal(770.0);
                p.setIva(161.7);
                p.setTotal(931.7);
                items.add(manualPresuLine(p, LONG_DESC, 10.0, 65.0, 650.0));
                items.add(manualPresuLine(p, "Mantenimiento mensual — revisión de filtros y registros.", 1.0, 120.0, 120.0));
            }
            case DEFAULT, LONG_FOOTER -> {
                p.setSubtotal(770.0);
                p.setIva(161.7);
                p.setTotal(931.7);
                p.setTieneAnticipo(true);
                p.setImporteAnticipo(new BigDecimal("150.00"));
                p.setAnticipoFacturado(true);
                p.setFechaAnticipo(LocalDate.of(2025, 3, 22));
                items.add(manualPresuLine(p, "Servicio de consultoría", 10.0, 65.0, 650.0));
                items.add(manualPresuLine(p, "Mantenimiento mensual", 1.0, 120.0, 120.0));
            }
        }
        p.setItems(items);
        return p;
    }

    private static PresupuestoItem manualPresuLine(
            Presupuesto presupuesto, String descripcion, double cantidad, double pu, double subtotal) {
        PresupuestoItem it = new PresupuestoItem();
        it.setPresupuesto(presupuesto);
        it.setEsTareaManual(true);
        it.setTareaManual(descripcion);
        it.setCantidad(cantidad);
        it.setPrecioUnitario(pu);
        it.setSubtotal(subtotal);
        it.setVisiblePdf(true);
        it.setAplicaIva(true);
        return it;
    }
}
