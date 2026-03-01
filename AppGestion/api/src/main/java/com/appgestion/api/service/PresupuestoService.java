package com.appgestion.api.service;

import com.appgestion.api.constant.TaxConstants;
import com.appgestion.api.domain.entity.*;
import com.appgestion.api.dto.request.PresupuestoItemRequest;
import com.appgestion.api.dto.request.PresupuestoRequest;
import com.appgestion.api.dto.request.EnviarEmailRequest;
import com.appgestion.api.dto.response.PresupuestoItemResponse;
import com.appgestion.api.dto.response.PresupuestoResponse;
import com.appgestion.api.repository.ClienteRepository;
import com.appgestion.api.repository.MaterialRepository;
import com.appgestion.api.repository.PresupuestoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.mail.MessagingException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class PresupuestoService {

    private final PresupuestoRepository presupuestoRepository;
    private final ClienteRepository clienteRepository;
    private final MaterialRepository materialRepository;
    private final PresupuestoPdfService presupuestoPdfService;
    private final EmailService emailService;

    public PresupuestoService(PresupuestoRepository presupuestoRepository,
                              ClienteRepository clienteRepository,
                              MaterialRepository materialRepository,
                              PresupuestoPdfService presupuestoPdfService,
                              EmailService emailService) {
        this.presupuestoRepository = presupuestoRepository;
        this.clienteRepository = clienteRepository;
        this.materialRepository = materialRepository;
        this.presupuestoPdfService = presupuestoPdfService;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public List<PresupuestoResponse> listar(Long usuarioId) {
        return presupuestoRepository.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PresupuestoResponse obtenerPorId(Long id, Long usuarioId) {
        Presupuesto presupuesto = presupuestoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Presupuesto no encontrado"));
        return toResponse(presupuesto);
    }

    @Transactional
    public PresupuestoResponse crear(PresupuestoRequest request, Usuario usuario) {
        Cliente cliente = clienteRepository.findByIdAndUsuarioId(request.clienteId(), usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));

        Presupuesto presupuesto = new Presupuesto();
        presupuesto.setUsuario(usuario);
        presupuesto.setCliente(cliente);
        presupuesto.setIvaHabilitado(request.ivaHabilitado());
        presupuesto.setEstado(request.estado());
        presupuesto.setDescuentoGlobalPorcentaje(request.descuentoGlobalPorcentaje());
        presupuesto.setDescuentoGlobalFijo(request.descuentoGlobalFijo());
        presupuesto.setDescuentoAntesIva(request.descuentoAntesIva());

        mapItems(request.items(), presupuesto);
        calcularTotales(presupuesto);
        presupuesto = presupuestoRepository.save(presupuesto);
        return toResponse(presupuesto);
    }

    @Transactional
    public PresupuestoResponse actualizar(Long id, PresupuestoRequest request, Long usuarioId) {
        Presupuesto presupuesto = presupuestoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Presupuesto no encontrado"));

        Cliente cliente = clienteRepository.findByIdAndUsuarioId(request.clienteId(), usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));

        presupuesto.setCliente(cliente);
        presupuesto.setIvaHabilitado(request.ivaHabilitado());
        presupuesto.setEstado(request.estado());
        presupuesto.setDescuentoGlobalPorcentaje(request.descuentoGlobalPorcentaje());
        presupuesto.setDescuentoGlobalFijo(request.descuentoGlobalFijo());
        presupuesto.setDescuentoAntesIva(request.descuentoAntesIva());

        presupuesto.getItems().clear();
        mapItems(request.items(), presupuesto);
        calcularTotales(presupuesto);
        presupuesto = presupuestoRepository.save(presupuesto);
        return toResponse(presupuesto);
    }

    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        if (!presupuestoRepository.existsByIdAndUsuarioId(Objects.requireNonNull(id), Objects.requireNonNull(usuarioId))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Presupuesto no encontrado");
        }
        presupuestoRepository.deleteById(Objects.requireNonNull(id));
    }

    @Transactional(readOnly = true)
    public byte[] generarPdf(Long id, Long usuarioId) {
        Presupuesto presupuesto = presupuestoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Presupuesto no encontrado"));
        return presupuestoPdfService.generarPdf(presupuesto, usuarioId);
    }

    @Transactional(readOnly = true)
    public void enviarPorEmail(Long id, Long usuarioId, EnviarEmailRequest request) throws MessagingException {
        Presupuesto presupuesto = presupuestoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Presupuesto no encontrado"));
        String email = request != null && request.email() != null && !request.email().isBlank()
                ? request.email().trim()
                : (presupuesto.getCliente() != null ? presupuesto.getCliente().getEmail() : null);
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cliente no tiene email registrado. Indique un email en el request.");
        }
        byte[] pdf = presupuestoPdfService.generarPdf(presupuesto, usuarioId);
        String nombreArchivo = "presupuesto-" + id + ".pdf";
        String asunto = "Presupuesto - " + (presupuesto.getCliente() != null ? presupuesto.getCliente().getNombre() : "");
        String cuerpo = "<p>Adjunto encontrará el presupuesto solicitado.</p><p>Saludos cordiales.</p>";
        emailService.enviarPdf(usuarioId, email, asunto, cuerpo, pdf, nombreArchivo);
    }

    private void mapItems(List<PresupuestoItemRequest> itemRequests, Presupuesto presupuesto) {
        for (PresupuestoItemRequest req : itemRequests) {
            PresupuestoItem item = new PresupuestoItem();
            item.setPresupuesto(presupuesto);
            double cantidad = Optional.ofNullable(req.cantidad()).orElse(0.0);
            double precioUnitario = Optional.ofNullable(req.precioUnitario()).orElse(0.0);
            item.setCantidad(cantidad);
            item.setPrecioUnitario(precioUnitario);
            item.setAplicaIva(Optional.ofNullable(req.aplicaIva()).orElse(true));
            item.setDescuentoPorcentaje(Optional.ofNullable(req.descuentoPorcentaje()).orElse(0.0));
            item.setDescuentoFijo(Optional.ofNullable(req.descuentoFijo()).orElse(0.0));
            item.setVisiblePdf(Optional.ofNullable(req.visiblePdf()).orElse(true));

            if (req.materialId() != null) {
                Long usuarioId = Objects.requireNonNull(presupuesto.getUsuario()).getId();
                materialRepository.findByIdAndUsuarioId(Objects.requireNonNull(req.materialId()), usuarioId).ifPresent(item::setMaterial);
                item.setEsTareaManual(false);
                item.setTareaManual(null);
            } else {
                item.setEsTareaManual(true);
                item.setTareaManual(req.tareaManual());
            }

            double descPctItem = Optional.ofNullable(req.descuentoPorcentaje()).orElse(0.0);
            double descFijoItem = Optional.ofNullable(req.descuentoFijo()).orElse(0.0);
            double itemSubtotal = cantidad * precioUnitario;
            itemSubtotal = itemSubtotal * (1 - descPctItem / 100) - descFijoItem;
            item.setSubtotal(Math.max(0, itemSubtotal));

            presupuesto.getItems().add(item);
        }
    }

    private void calcularTotales(Presupuesto presupuesto) {
        double subtotal = 0;
        double baseIva = 0;

        for (PresupuestoItem item : presupuesto.getItems()) {
            subtotal += item.getSubtotal();
            if (Boolean.TRUE.equals(item.getAplicaIva())) {
                baseIva += item.getSubtotal();
            }
        }

        double descPct = Optional.ofNullable(presupuesto.getDescuentoGlobalPorcentaje()).orElse(0.0);
        double descFijo = Optional.ofNullable(presupuesto.getDescuentoGlobalFijo()).orElse(0.0);

        if (Boolean.TRUE.equals(presupuesto.getDescuentoAntesIva())) {
            subtotal = subtotal * (1 - descPct / 100) - descFijo;
            baseIva = baseIva * (1 - descPct / 100) - descFijo;
        } else {
            subtotal = subtotal * (1 - descPct / 100) - descFijo;
        }

        subtotal = Math.max(0, subtotal);
        baseIva = Math.max(0, baseIva);

        presupuesto.setSubtotal(subtotal);
        double iva = Boolean.TRUE.equals(presupuesto.getIvaHabilitado()) ? baseIva * TaxConstants.IVA_RATE_DOUBLE : 0;
        presupuesto.setIva(iva);
        presupuesto.setTotal(subtotal + iva);
    }

    private PresupuestoResponse toResponse(Presupuesto presupuesto) {
        List<PresupuestoItemResponse> items = presupuesto.getItems().stream()
                .map(item -> new PresupuestoItemResponse(
                        item.getId(),
                        item.getMaterial() != null ? item.getMaterial().getId() : null,
                        item.getEsTareaManual() != null && item.getEsTareaManual() ? item.getTareaManual() : (item.getMaterial() != null ? item.getMaterial().getNombre() : ""),
                        item.getEsTareaManual() != null && item.getEsTareaManual(),
                        item.getCantidad(),
                        item.getPrecioUnitario(),
                        item.getSubtotal(),
                        Optional.ofNullable(item.getVisiblePdf()).orElse(true)
                ))
                .toList();

        return new PresupuestoResponse(
                presupuesto.getId(),
                Objects.requireNonNull(Objects.requireNonNull(presupuesto.getCliente()).getId()),
                presupuesto.getCliente().getNombre(),
                presupuesto.getCliente().getEmail(),
                presupuesto.getFechaCreacion(),
                presupuesto.getSubtotal(),
                presupuesto.getIva(),
                presupuesto.getTotal(),
                presupuesto.getIvaHabilitado(),
                presupuesto.getEstado(),
                Optional.ofNullable(presupuesto.getDescuentoGlobalPorcentaje()).orElse(0.0),
                Optional.ofNullable(presupuesto.getDescuentoGlobalFijo()).orElse(0.0),
                Optional.ofNullable(presupuesto.getDescuentoAntesIva()).orElse(true),
                items
        );
    }
}
