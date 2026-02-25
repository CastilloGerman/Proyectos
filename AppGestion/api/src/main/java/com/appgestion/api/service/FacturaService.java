package com.appgestion.api.service;

import com.appgestion.api.domain.entity.*;
import com.appgestion.api.dto.request.FacturaItemRequest;
import com.appgestion.api.dto.request.FacturaRequest;
import com.appgestion.api.dto.request.EnviarEmailRequest;
import com.appgestion.api.dto.response.FacturaItemResponse;
import com.appgestion.api.dto.response.FacturaResponse;
import com.appgestion.api.repository.ClienteRepository;
import com.appgestion.api.repository.FacturaRepository;
import com.appgestion.api.repository.MaterialRepository;
import com.appgestion.api.repository.PresupuestoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.mail.MessagingException;
import java.time.Year;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class FacturaService {

    private static final double IVA_RATE = 0.21;

    private final FacturaRepository facturaRepository;
    private final ClienteRepository clienteRepository;
    private final PresupuestoRepository presupuestoRepository;
    private final MaterialRepository materialRepository;
    private final FacturaPdfService facturaPdfService;
    private final EmailService emailService;

    public FacturaService(FacturaRepository facturaRepository,
                          ClienteRepository clienteRepository,
                          PresupuestoRepository presupuestoRepository,
                          MaterialRepository materialRepository,
                          FacturaPdfService facturaPdfService,
                          EmailService emailService) {
        this.facturaRepository = facturaRepository;
        this.clienteRepository = clienteRepository;
        this.presupuestoRepository = presupuestoRepository;
        this.materialRepository = materialRepository;
        this.facturaPdfService = facturaPdfService;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public List<FacturaResponse> listar(Long usuarioId) {
        return facturaRepository.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FacturaResponse obtenerPorId(Long id, Long usuarioId) {
        Factura factura = facturaRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada"));
        return toResponse(factura);
    }

    @Transactional
    public FacturaResponse crear(FacturaRequest request, Usuario usuario) {
        Cliente cliente = clienteRepository.findByIdAndUsuarioId(request.clienteId(), usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));

        Presupuesto presupuesto = null;
        if (request.presupuestoId() != null) {
            presupuesto = presupuestoRepository.findByIdAndUsuarioId(request.presupuestoId(), usuario.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Presupuesto no encontrado"));
            presupuesto.setEstado("Aceptado");
        }

        String numeroFactura = request.numeroFactura();
        if (numeroFactura == null || numeroFactura.isBlank()) {
            numeroFactura = generarNumeroFactura(usuario.getId());
        } else {
            if (facturaRepository.findByNumeroFacturaAndUsuarioId(numeroFactura, usuario.getId()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe una factura con ese número");
            }
        }

        Factura factura = new Factura();
        factura.setUsuario(usuario);
        factura.setNumeroFactura(numeroFactura);
        factura.setCliente(cliente);
        factura.setPresupuesto(presupuesto);
        factura.setFechaVencimiento(request.fechaVencimiento());
        factura.setMetodoPago(request.metodoPago());
        factura.setEstadoPago(request.estadoPago());
        factura.setNotas(request.notas());
        factura.setIvaHabilitado(request.ivaHabilitado());

        mapItems(request.items(), factura);
        calcularTotales(factura);
        factura = facturaRepository.save(factura);
        return toResponse(factura);
    }

    @Transactional
    public FacturaResponse actualizar(Long id, FacturaRequest request, Long usuarioId) {
        Factura factura = facturaRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada"));

        Cliente cliente = clienteRepository.findByIdAndUsuarioId(request.clienteId(), usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));

        Presupuesto presupuesto = null;
        if (request.presupuestoId() != null) {
            presupuesto = presupuestoRepository.findByIdAndUsuarioId(request.presupuestoId(), usuarioId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Presupuesto no encontrado"));
            presupuesto.setEstado("Aceptado");
        }

        if (request.numeroFactura() != null && !request.numeroFactura().isBlank() && !request.numeroFactura().equals(factura.getNumeroFactura())) {
            if (facturaRepository.findByNumeroFacturaAndUsuarioId(request.numeroFactura(), usuarioId).isPresent()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe una factura con ese número");
            }
            factura.setNumeroFactura(request.numeroFactura());
        }

        factura.setCliente(cliente);
        factura.setPresupuesto(presupuesto);
        factura.setFechaVencimiento(request.fechaVencimiento());
        factura.setMetodoPago(request.metodoPago());
        factura.setEstadoPago(request.estadoPago());
        factura.setNotas(request.notas());
        factura.setIvaHabilitado(request.ivaHabilitado());

        factura.getItems().clear();
        mapItems(request.items(), factura);
        calcularTotales(factura);
        factura = facturaRepository.save(factura);
        return toResponse(factura);
    }

    @Transactional
    public FacturaResponse crearDesdePresupuesto(Long presupuestoId, Usuario usuario) {
        Presupuesto presupuesto = presupuestoRepository.findByIdAndUsuarioId(presupuestoId, usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Presupuesto no encontrado"));

        String numeroFactura = generarNumeroFactura(usuario.getId());

        Factura factura = new Factura();
        factura.setUsuario(usuario);
        factura.setNumeroFactura(numeroFactura);
        factura.setCliente(presupuesto.getCliente());
        factura.setPresupuesto(presupuesto);
        factura.setIvaHabilitado(presupuesto.getIvaHabilitado());
        factura.setMetodoPago("Transferencia");
        factura.setEstadoPago("No Pagada");

        for (PresupuestoItem pi : presupuesto.getItems()) {
            FacturaItem item = new FacturaItem();
            item.setFactura(factura);
            item.setMaterial(pi.getMaterial());
            item.setTareaManual(pi.getTareaManual());
            item.setEsTareaManual(Optional.ofNullable(pi.getEsTareaManual()).orElse(false));
            item.setCantidad(Optional.ofNullable(pi.getCantidad()).orElse(0.0));
            item.setPrecioUnitario(Optional.ofNullable(pi.getPrecioUnitario()).orElse(0.0));
            item.setSubtotal(Optional.ofNullable(pi.getSubtotal()).orElse(0.0));
            item.setAplicaIva(Optional.ofNullable(pi.getAplicaIva()).orElse(true));
            factura.getItems().add(item);
        }

        presupuesto.setEstado("Aceptado");
        calcularTotales(factura);
        factura = facturaRepository.save(factura);
        return toResponse(factura);
    }

    @Transactional(readOnly = true)
    public byte[] generarPdf(Long id, Long usuarioId) {
        Factura factura = facturaRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada"));
        return facturaPdfService.generarPdf(factura, usuarioId);
    }

    @Transactional(readOnly = true)
    public void enviarPorEmail(Long id, Long usuarioId, EnviarEmailRequest request) throws MessagingException {
        Factura factura = facturaRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada"));
        String email = request != null && request.email() != null && !request.email().isBlank()
                ? request.email().trim()
                : (factura.getCliente() != null ? factura.getCliente().getEmail() : null);
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cliente no tiene email registrado. Indique un email en el request.");
        }
        byte[] pdf = facturaPdfService.generarPdf(factura, usuarioId);
        String nombreArchivo = "factura-" + (factura.getNumeroFactura() != null ? factura.getNumeroFactura() : id) + ".pdf";
        String asunto = "Factura " + factura.getNumeroFactura() + " - " + (factura.getCliente() != null ? factura.getCliente().getNombre() : "");
        String cuerpo = "<p>Adjunto encontrará la factura correspondiente.</p><p>Saludos cordiales.</p>";
        emailService.enviarPdf(email, asunto, cuerpo, pdf, nombreArchivo);
    }

    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        if (!facturaRepository.existsByIdAndUsuarioId(Objects.requireNonNull(id), Objects.requireNonNull(usuarioId))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada");
        }
        facturaRepository.deleteById(Objects.requireNonNull(id));
    }

    private String generarNumeroFactura(Long usuarioId) {
        long count = facturaRepository.countByUsuarioId(Objects.requireNonNull(usuarioId));
        int year = Year.now().getValue();
        return String.format("FAC-%d-%04d", year, count + 1);
    }

    private void mapItems(List<FacturaItemRequest> itemRequests, Factura factura) {
        for (FacturaItemRequest req : itemRequests) {
            FacturaItem item = new FacturaItem();
            item.setFactura(factura);
            double cantidad = Optional.ofNullable(req.cantidad()).orElse(0.0);
            double precioUnitario = Optional.ofNullable(req.precioUnitario()).orElse(0.0);
            item.setCantidad(cantidad);
            item.setPrecioUnitario(precioUnitario);
            item.setAplicaIva(Optional.ofNullable(req.aplicaIva()).orElse(true));

            if (req.materialId() != null) {
                Long usuarioId = Objects.requireNonNull(factura.getUsuario()).getId();
                materialRepository.findByIdAndUsuarioId(Objects.requireNonNull(req.materialId()), usuarioId).ifPresent(item::setMaterial);
                item.setEsTareaManual(false);
                item.setTareaManual(null);
            } else {
                item.setEsTareaManual(true);
                item.setTareaManual(req.tareaManual());
            }

            double itemSubtotal = cantidad * precioUnitario;
            item.setSubtotal(itemSubtotal);
            item.setCuotaIva(0.0);

            factura.getItems().add(item);
        }
    }

    private void calcularTotales(Factura factura) {
        double subtotal = 0;
        double baseIva = 0;

        for (FacturaItem item : factura.getItems()) {
            subtotal += item.getSubtotal();
            if (Boolean.TRUE.equals(item.getAplicaIva())) {
                baseIva += item.getSubtotal();
            }
        }

        factura.setSubtotal(subtotal);
        double iva = Boolean.TRUE.equals(factura.getIvaHabilitado()) ? baseIva * IVA_RATE : 0;
        factura.setIva(iva);
        factura.setTotal(subtotal + iva);

        if (Boolean.TRUE.equals(factura.getIvaHabilitado())) {
            double totalIva = iva;
            double totalBaseIva = baseIva;
            for (FacturaItem item : factura.getItems()) {
                if (Boolean.TRUE.equals(item.getAplicaIva()) && totalBaseIva > 0) {
                    double cuotaIva = item.getSubtotal() * (totalIva / totalBaseIva);
                    item.setCuotaIva(cuotaIva);
                }
            }
        }
    }

    private FacturaResponse toResponse(Factura factura) {
        List<FacturaItemResponse> items = factura.getItems().stream()
                .map(item -> new FacturaItemResponse(
                        item.getId(),
                        item.getMaterial() != null ? item.getMaterial().getId() : null,
                        item.getEsTareaManual() != null && item.getEsTareaManual() ? item.getTareaManual() : (item.getMaterial() != null ? item.getMaterial().getNombre() : ""),
                        item.getEsTareaManual() != null && item.getEsTareaManual(),
                        item.getCantidad(),
                        item.getPrecioUnitario(),
                        item.getSubtotal()
                ))
                .toList();

        return new FacturaResponse(
                factura.getId(),
                factura.getNumeroFactura(),
                factura.getCliente().getId(),
                factura.getCliente().getNombre(),
                factura.getCliente().getEmail(),
                factura.getPresupuesto() != null ? factura.getPresupuesto().getId() : null,
                factura.getFechaCreacion(),
                factura.getFechaVencimiento(),
                factura.getSubtotal(),
                factura.getIva(),
                factura.getTotal(),
                factura.getIvaHabilitado(),
                factura.getMetodoPago(),
                factura.getEstadoPago(),
                factura.getNotas(),
                items
        );
    }
}
