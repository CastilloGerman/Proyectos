package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Factura;
import com.appgestion.api.domain.entity.Presupuesto;
import com.appgestion.api.dto.response.ClienteFacturaResumen;
import com.appgestion.api.dto.response.ClienteHistorialItem;
import com.appgestion.api.dto.response.ClientePanelResponse;
import com.appgestion.api.dto.response.ClientePresupuestoResumen;
import com.appgestion.api.dto.response.ClienteResponse;
import com.appgestion.api.repository.ClienteRepository;
import com.appgestion.api.repository.FacturaRepository;
import com.appgestion.api.repository.PresupuestoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class ClientePanelService {

    private static final String RECHAZADO = "Rechazado";

    private final ClienteRepository clienteRepository;
    private final ClienteService clienteService;
    private final FacturaRepository facturaRepository;
    private final PresupuestoRepository presupuestoRepository;

    public ClientePanelService(
            ClienteRepository clienteRepository,
            ClienteService clienteService,
            FacturaRepository facturaRepository,
            PresupuestoRepository presupuestoRepository
    ) {
        this.clienteRepository = clienteRepository;
        this.clienteService = clienteService;
        this.facturaRepository = facturaRepository;
        this.presupuestoRepository = presupuestoRepository;
    }

    @Transactional(readOnly = true)
    public ClientePanelResponse obtenerPanel(Long clienteId, Long usuarioId) {
        if (!clienteRepository.existsByIdAndUsuarioId(Objects.requireNonNull(clienteId), Objects.requireNonNull(usuarioId))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado");
        }
        ClienteResponse cliente = clienteService.obtenerPorId(clienteId, usuarioId);

        List<Factura> facturas = facturaRepository.findByUsuarioIdAndClienteIdOrderByFechaCreacionDesc(usuarioId, clienteId);
        List<Presupuesto> presupuestos = presupuestoRepository.findByUsuarioIdAndClienteIdOrderByFechaCreacionDesc(usuarioId, clienteId);

        Map<Long, Long> presupuestoToFactura = new HashMap<>();
        for (Object[] row : facturaRepository.findPresupuestoFacturaIdPairsByCliente(usuarioId, clienteId)) {
            if (row == null || row.length < 2) {
                continue;
            }
            Long pid = (Long) row[0];
            Long fid = (Long) row[1];
            if (pid != null && fid != null) {
                presupuestoToFactura.putIfAbsent(pid, fid);
            }
        }

        List<ClienteFacturaResumen> facturasRes = facturas.stream()
                .map(this::toFacturaResumen)
                .toList();

        double totalPendiente = facturasRes.stream().mapToDouble(ClienteFacturaResumen::pendiente).sum();
        int facturasConPendiente = (int) facturasRes.stream().filter(f -> f.pendiente() > 0.001).count();

        List<ClientePresupuestoResumen> presRes = presupuestos.stream()
                .map(p -> toPresupuestoResumen(p, presupuestoToFactura.get(p.getId())))
                .toList();

        List<ClientePresupuestoResumen> activos = presRes.stream()
                .filter(ClientePresupuestoResumen::activo)
                .toList();

        List<ClienteHistorialItem> historial = new ArrayList<>();
        for (Factura f : facturas) {
            double pend = pendienteFactura(f);
            String sub = "";
            if (pend > 0.001) {
                sub = String.format(Locale.ROOT, "Pendiente %.2f €", pend);
            }
            if (f.getFechaVencimiento() != null) {
                sub = sub.isEmpty()
                        ? "Venc. " + f.getFechaVencimiento()
                        : sub + " · Venc. " + f.getFechaVencimiento();
            }
            historial.add(new ClienteHistorialItem(
                    "FACTURA",
                    f.getId(),
                    f.getFechaCreacion(),
                    f.getNumeroFactura(),
                    Optional.ofNullable(f.getTotal()).orElse(0.0),
                    f.getEstadoPago(),
                    sub
            ));
        }
        for (Presupuesto p : presupuestos) {
            historial.add(new ClienteHistorialItem(
                    "PRESUPUESTO",
                    p.getId(),
                    p.getFechaCreacion(),
                    "Presupuesto #" + p.getId(),
                    Optional.ofNullable(p.getTotal()).orElse(0.0),
                    p.getEstado(),
                    presupuestoToFactura.containsKey(p.getId()) ? "Factura generada" : ""
            ));
        }
        historial.sort(Comparator.comparing(ClienteHistorialItem::fechaOrden).reversed());

        return new ClientePanelResponse(
                cliente,
                totalPendiente,
                facturasConPendiente,
                facturasRes,
                presRes,
                activos,
                historial
        );
    }

    private ClienteFacturaResumen toFacturaResumen(Factura f) {
        double pend = pendienteFactura(f);
        return new ClienteFacturaResumen(
                f.getId(),
                f.getNumeroFactura(),
                f.getFechaCreacion(),
                f.getFechaExpedicion(),
                f.getFechaVencimiento(),
                f.getTotal(),
                f.getEstadoPago(),
                f.getMontoCobrado(),
                pend
        );
    }

    private ClientePresupuestoResumen toPresupuestoResumen(Presupuesto p, Long facturaId) {
        String estado = p.getEstado();
        boolean activo = estado == null || !RECHAZADO.equalsIgnoreCase(estado.trim());
        return new ClientePresupuestoResumen(
                p.getId(),
                p.getFechaCreacion(),
                p.getTotal(),
                estado,
                facturaId,
                activo
        );
    }

    private static double pendienteFactura(Factura f) {
        String ep = f.getEstadoPago();
        if (ep != null && ep.equalsIgnoreCase("Pagada")) {
            return 0.0;
        }
        double total = Optional.ofNullable(f.getTotal()).orElse(0.0);
        double cobrado = Optional.ofNullable(f.getMontoCobrado()).orElse(0.0);
        if (ep != null && ep.equalsIgnoreCase("Parcial")) {
            return Math.max(0.0, total - cobrado);
        }
        return total;
    }
}
