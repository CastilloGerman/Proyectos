package com.appgestion.api.service;

import com.appgestion.api.constant.TaxConstants;
import com.appgestion.api.domain.entity.*;
import com.appgestion.api.domain.enums.TipoFactura;
import com.appgestion.api.dto.response.AnticipoResumenDTO;
import com.appgestion.api.dto.response.FacturaResponse;
import com.appgestion.api.repository.FacturaCobroRepository;
import com.appgestion.api.repository.FacturaRepository;
import com.appgestion.api.repository.PresupuestoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Flujo contable de anticipos: registro, factura de anticipo (IVA en trimestre del cobro)
 * y factura final con base/IVA remanentes y descuento del anticipo ya facturado.
 */
@Service
public class AnticipoService {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final PresupuestoRepository presupuestoRepository;
    private final FacturaRepository facturaRepository;
    private final FacturaNumeroService facturaNumeroService;
    private final FacturaCobroRepository facturaCobroRepository;
    private final FacturaResponseMapper facturaResponseMapper;
    private final FacturaService facturaService;

    public AnticipoService(
            PresupuestoRepository presupuestoRepository,
            FacturaRepository facturaRepository,
            FacturaNumeroService facturaNumeroService,
            FacturaCobroRepository facturaCobroRepository,
            FacturaResponseMapper facturaResponseMapper,
            FacturaService facturaService) {
        this.presupuestoRepository = presupuestoRepository;
        this.facturaRepository = facturaRepository;
        this.facturaNumeroService = facturaNumeroService;
        this.facturaCobroRepository = facturaCobroRepository;
        this.facturaResponseMapper = facturaResponseMapper;
        this.facturaService = facturaService;
    }

    @Transactional
    public void registrarAnticipo(Long presupuestoId, BigDecimal importeAnticipo, LocalDate fechaAnticipo, Long usuarioId) {
        Presupuesto p = presupuestoRepository.findByIdAndUsuarioId(presupuestoId, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Presupuesto no encontrado"));
        if (!"Aceptado".equalsIgnoreCase(Optional.ofNullable(p.getEstado()).orElse("").trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El anticipo solo se registra con el presupuesto en estado Aceptado");
        }
        if (Boolean.TRUE.equals(p.getAnticipoFacturado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El anticipo ya está facturado; no se puede modificar");
        }
        if (importeAnticipo == null || importeAnticipo.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El importe del anticipo debe ser mayor que cero");
        }
        BigDecimal totalPres = BigDecimal.valueOf(Optional.ofNullable(p.getTotal()).orElse(0.0)).setScale(SCALE, ROUNDING);
        if (importeAnticipo.compareTo(totalPres) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El anticipo no puede superar el total del presupuesto");
        }
        p.setTieneAnticipo(true);
        p.setImporteAnticipo(importeAnticipo.setScale(SCALE, ROUNDING));
        p.setFechaAnticipo(fechaAnticipo);
        presupuestoRepository.save(p);
    }

    @Transactional
    public FacturaResponse generarFacturaAnticipo(Long presupuestoId, Long usuarioId, Usuario usuario) {
        Presupuesto p = presupuestoRepository.findByIdAndUsuarioId(presupuestoId, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Presupuesto no encontrado"));
        if (!Boolean.TRUE.equals(p.getTieneAnticipo()) || p.getImporteAnticipo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No hay anticipo registrado en el presupuesto");
        }
        if (Boolean.TRUE.equals(p.getAnticipoFacturado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La factura de anticipo ya fue generada");
        }
        validarPresupuestoHomogeneoIva21(p);
        Cliente cliente = p.getCliente();
        facturaService.validarEmisionFactura(usuario.getId(), cliente);

        BigDecimal importe = p.getImporteAnticipo().setScale(SCALE, ROUNDING);
        LocalDate fecha = Objects.requireNonNull(p.getFechaAnticipo(), "fecha anticipo");

        BigDecimal divisor = BigDecimal.ONE.add(TaxConstants.IVA_RATE);
        BigDecimal baseAnticipo = importe.divide(divisor, SCALE, ROUNDING);

        Factura f = new Factura();
        f.setUsuario(usuario);
        f.setNumeroFactura(facturaNumeroService.generarSiguienteNumero(usuario.getId()));
        f.setCliente(cliente);
        f.setPresupuesto(p);
        f.setTipoFactura(TipoFactura.ANTICIPO);
        f.setFechaExpedicion(fecha);
        f.setFechaOperacion(fecha);
        f.setRegimenFiscal("Régimen general del IVA");
        f.setMoneda("EUR");
        f.setIvaHabilitado(true);
        f.setMetodoPago("Transferencia");
        f.setEstadoPago("Pagada");
        f.setImporteAnticipoDescontado(null);

        FacturaItem linea = new FacturaItem();
        linea.setFactura(f);
        linea.setEsTareaManual(true);
        linea.setTareaManual("Anticipo trabajos — " + descripcionBrevePresupuesto(p));
        linea.setCantidad(1.0);
        linea.setPrecioUnitario(baseAnticipo.doubleValue());
        linea.setSubtotal(baseAnticipo.doubleValue());
        linea.setAplicaIva(true);
        linea.setCuotaIva(0.0);
        f.getItems().add(linea);

        facturaService.recalcularTotales(f);

        f = facturaRepository.save(f);

        FacturaCobro cobro = new FacturaCobro();
        cobro.setFactura(f);
        cobro.setImporte(importe.doubleValue());
        cobro.setFecha(fecha);
        cobro.setMetodo("Transferencia");
        cobro.setNotas("Cobro del anticipo (criterio caja: fecha de cobro)");
        facturaCobroRepository.save(cobro);
        f.setMontoCobrado(importe.doubleValue());
        facturaRepository.save(f);

        p.setAnticipoFacturado(true);
        presupuestoRepository.save(p);

        return facturaResponseMapper.toResponse(f,
                facturaCobroRepository.findByFacturaIdOrderByFechaDescCreatedAtDesc(f.getId()));
    }

    /**
     * Emite la factura final (restante). Si el anticipo está registrado pero aún no se emitió la factura de anticipo,
     * la genera primero (mismo criterio legal: dos facturas, anticipo con fecha de cobro registrada).
     */
    @Transactional
    public FacturaResponse generarFacturaFinal(Long presupuestoId, Long usuarioId, Usuario usuario) {
        Presupuesto p = presupuestoRepository.findByIdAndUsuarioId(presupuestoId, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Presupuesto no encontrado"));
        if (!Boolean.TRUE.equals(p.getTieneAnticipo()) || p.getImporteAnticipo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No hay anticipo registrado");
        }
        if (!Boolean.TRUE.equals(p.getAnticipoFacturado())) {
            // Misma transacción que la factura final (invocación interna; el anticipo debe existir antes del remanente).
            generarFacturaAnticipo(presupuestoId, usuarioId, usuario);
            p = presupuestoRepository.findByIdAndUsuarioId(presupuestoId, usuarioId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Presupuesto no encontrado"));
        }
        Factura fa = facturaRepository.findByPresupuesto_IdAndUsuario_IdAndTipoFactura(presupuestoId, usuarioId, TipoFactura.ANTICIPO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se encontró la factura de anticipo"));
        if (!facturaRepository.findVentasPrincipalesPorPresupuesto(
                presupuestoId, usuarioId, TipoFactura.NORMAL, TipoFactura.FINAL_CON_ANTICIPO).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este presupuesto ya tiene factura de venta principal");
        }
        validarPresupuestoHomogeneoIva21(p);
        Cliente cliente = p.getCliente();
        facturaService.validarEmisionFactura(usuario.getId(), cliente);

        BigDecimal baseTotal = BigDecimal.valueOf(Optional.ofNullable(p.getSubtotal()).orElse(0.0)).setScale(SCALE, ROUNDING);

        BigDecimal baseAnt = BigDecimal.valueOf(Optional.ofNullable(fa.getSubtotal()).orElse(0.0)).setScale(SCALE, ROUNDING);
        BigDecimal importeAnt = p.getImporteAnticipo().setScale(SCALE, ROUNDING);

        BigDecimal baseRest = baseTotal.subtract(baseAnt).max(BigDecimal.ZERO).setScale(SCALE, ROUNDING);

        Factura f = new Factura();
        f.setUsuario(usuario);
        f.setNumeroFactura(facturaNumeroService.generarSiguienteNumero(usuario.getId()));
        f.setCliente(cliente);
        f.setPresupuesto(p);
        f.setTipoFactura(TipoFactura.FINAL_CON_ANTICIPO);
        f.setFacturaAnticipoReferencia(fa);
        f.setImporteAnticipoDescontado(importeAnt);
        f.setFechaExpedicion(LocalDate.now());
        f.setRegimenFiscal("Régimen general del IVA");
        f.setMoneda("EUR");
        f.setIvaHabilitado(true);
        f.setMetodoPago("Transferencia");
        f.setEstadoPago("No Pagada");

        construirLineasFacturaFinalProporcionales(p, f, baseRest.doubleValue());

        facturaService.recalcularTotales(f);

        p.setEstado("Aceptado");
        presupuestoRepository.save(p);

        f = facturaRepository.save(f);
        return facturaResponseMapper.toResponse(f,
                facturaCobroRepository.findByFacturaIdOrderByFechaDescCreatedAtDesc(f.getId()));
    }

    @Transactional(readOnly = true)
    public AnticipoResumenDTO calcularResumenAnticipo(Long presupuestoId, Long usuarioId) {
        Presupuesto p = presupuestoRepository.findByIdAndUsuarioId(presupuestoId, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Presupuesto no encontrado"));
        BigDecimal totalPres = BigDecimal.valueOf(Optional.ofNullable(p.getTotal()).orElse(0.0)).setScale(SCALE, ROUNDING);
        boolean tiene = Boolean.TRUE.equals(p.getTieneAnticipo()) && p.getImporteAnticipo() != null
                && p.getImporteAnticipo().compareTo(BigDecimal.ZERO) > 0;
        if (!tiene) {
            return new AnticipoResumenDTO(
                    totalPres, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    totalPres, BigDecimal.valueOf(Optional.ofNullable(p.getSubtotal()).orElse(0.0)).setScale(SCALE, ROUNDING),
                    BigDecimal.valueOf(Optional.ofNullable(p.getIva()).orElse(0.0)).setScale(SCALE, ROUNDING),
                    false, false);
        }
        BigDecimal importeAnt = p.getImporteAnticipo().setScale(SCALE, ROUNDING);
        BigDecimal divisor = BigDecimal.ONE.add(TaxConstants.IVA_RATE);
        BigDecimal baseAnt = importeAnt.divide(divisor, SCALE, ROUNDING);
        BigDecimal ivaAnt = importeAnt.subtract(baseAnt).setScale(SCALE, ROUNDING);

        BigDecimal baseTotal = BigDecimal.valueOf(Optional.ofNullable(p.getSubtotal()).orElse(0.0)).setScale(SCALE, ROUNDING);
        BigDecimal ivaTotal = BigDecimal.valueOf(Optional.ofNullable(p.getIva()).orElse(0.0)).setScale(SCALE, ROUNDING);

        BigDecimal basePend;
        BigDecimal ivaPend;
        BigDecimal importePend;
        if (Boolean.TRUE.equals(p.getAnticipoFacturado())) {
            Optional<Factura> faOpt = facturaRepository.findByPresupuesto_IdAndUsuario_IdAndTipoFactura(
                    presupuestoId, usuarioId, TipoFactura.ANTICIPO);
            if (faOpt.isPresent()) {
                Factura fa = faOpt.get();
                baseAnt = BigDecimal.valueOf(Optional.ofNullable(fa.getSubtotal()).orElse(0.0)).setScale(SCALE, ROUNDING);
                ivaAnt = BigDecimal.valueOf(Optional.ofNullable(fa.getIva()).orElse(0.0)).setScale(SCALE, ROUNDING);
            }
            basePend = baseTotal.subtract(baseAnt).max(BigDecimal.ZERO).setScale(SCALE, ROUNDING);
            ivaPend = ivaTotal.subtract(ivaAnt).max(BigDecimal.ZERO).setScale(SCALE, ROUNDING);
            importePend = basePend.add(ivaPend).subtract(importeAnt).setScale(SCALE, ROUNDING);
        } else {
            basePend = baseTotal.subtract(baseAnt).max(BigDecimal.ZERO).setScale(SCALE, ROUNDING);
            ivaPend = ivaTotal.subtract(ivaAnt).max(BigDecimal.ZERO).setScale(SCALE, ROUNDING);
            importePend = basePend.add(ivaPend).setScale(SCALE, ROUNDING);
        }

        return new AnticipoResumenDTO(
                totalPres, importeAnt, baseAnt, ivaAnt, importePend, basePend, ivaPend,
                Boolean.TRUE.equals(p.getAnticipoFacturado()), true);
    }

    private void validarPresupuestoHomogeneoIva21(Presupuesto p) {
        if (!Boolean.TRUE.equals(p.getIvaHabilitado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El anticipo fiscal en esta versión requiere IVA habilitado al 21 % en todo el presupuesto");
        }
        for (PresupuestoItem it : p.getItems()) {
            if (!Boolean.TRUE.equals(it.getAplicaIva())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El anticipo fiscal requiere que todas las líneas repercutan IVA al 21 % (sin líneas exentas o mixtas en esta versión)");
            }
        }
    }

    private static String descripcionBrevePresupuesto(Presupuesto p) {
        return p.getItems().stream()
                .filter(it -> Optional.ofNullable(it.getVisiblePdf()).orElse(true))
                .map(it -> it.getEsTareaManual() != null && it.getEsTareaManual()
                        ? Optional.ofNullable(it.getTareaManual()).orElse("")
                        : (it.getMaterial() != null ? it.getMaterial().getNombre() : ""))
                .filter(s -> !s.isBlank())
                .findFirst()
                .orElse("conforme presupuesto");
    }

    private void construirLineasFacturaFinalProporcionales(Presupuesto presupuesto, Factura factura, double baseRestanteObjetivo) {
        List<PresupuestoItem> items = presupuesto.getItems();
        double sumaItems = items.stream().mapToDouble(i -> Optional.ofNullable(i.getSubtotal()).orElse(0.0)).sum();
        if (sumaItems <= 0 || baseRestanteObjetivo <= 0) {
            FacturaItem linea = new FacturaItem();
            linea.setFactura(factura);
            linea.setEsTareaManual(true);
            linea.setTareaManual("Prestación — importe pendiente tras anticipo");
            linea.setCantidad(1.0);
            linea.setPrecioUnitario(baseRestanteObjetivo);
            linea.setSubtotal(baseRestanteObjetivo);
            linea.setAplicaIva(true);
            linea.setCuotaIva(0.0);
            factura.getItems().add(linea);
            return;
        }
        BigDecimal baseRest = BigDecimal.valueOf(baseRestanteObjetivo).setScale(SCALE, ROUNDING);
        BigDecimal acum = BigDecimal.ZERO;
        for (int i = 0; i < items.size(); i++) {
            PresupuestoItem pi = items.get(i);
            double peso = Optional.ofNullable(pi.getSubtotal()).orElse(0.0) / sumaItems;
            BigDecimal parte;
            if (i < items.size() - 1) {
                parte = baseRest.multiply(BigDecimal.valueOf(peso)).setScale(SCALE, ROUNDING);
                acum = acum.add(parte);
            } else {
                parte = baseRest.subtract(acum).max(BigDecimal.ZERO).setScale(SCALE, ROUNDING);
            }
            if (parte.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            FacturaItem fi = new FacturaItem();
            fi.setFactura(factura);
            if (pi.getMaterial() != null) {
                fi.setMaterial(pi.getMaterial());
                fi.setEsTareaManual(false);
                fi.setTareaManual(null);
            } else {
                fi.setEsTareaManual(true);
                fi.setTareaManual(Optional.ofNullable(pi.getTareaManual()).orElse("Concepto"));
            }
            fi.setCantidad(Optional.ofNullable(pi.getCantidad()).orElse(1.0));
            double pu = parte.divide(BigDecimal.valueOf(fi.getCantidad()), 4, ROUNDING).doubleValue();
            fi.setPrecioUnitario(pu);
            fi.setSubtotal(parte.doubleValue());
            fi.setAplicaIva(true);
            fi.setCuotaIva(0.0);
            factura.getItems().add(fi);
        }
    }
}
