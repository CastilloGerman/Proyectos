package com.appgestion.api.service;

import com.appgestion.api.constant.PresupuestoEstado;
import com.appgestion.api.constant.TaxConstants;
import com.appgestion.api.domain.entity.*;
import com.appgestion.api.domain.enums.TipoFactura;
import com.appgestion.api.dto.request.PresupuestoEstadoPatchRequest;
import com.appgestion.api.dto.request.PresupuestoItemRequest;
import com.appgestion.api.dto.request.PresupuestoRequest;
import com.appgestion.api.dto.request.EnviarEmailRequest;
import com.appgestion.api.dto.response.PresupuestoItemResponse;
import com.appgestion.api.dto.response.PresupuestoResponse;
import com.appgestion.api.repository.ClienteRepository;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.repository.FacturaRepository;
import com.appgestion.api.repository.MaterialRepository;
import com.appgestion.api.repository.PresupuestoRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.util.EmailCopy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class PresupuestoService {

    private final PresupuestoRepository presupuestoRepository;
    private final ClienteRepository clienteRepository;
    private final EmpresaRepository empresaRepository;
    private final MaterialRepository materialRepository;
    private final FacturaRepository facturaRepository;
    private final PresupuestoPdfService presupuestoPdfService;
    private final EmailService emailService;
    private final PresupuestoCondicionesService presupuestoCondicionesService;
    private final UsuarioRepository usuarioRepository;

    public PresupuestoService(PresupuestoRepository presupuestoRepository,
                              ClienteRepository clienteRepository,
                              EmpresaRepository empresaRepository,
                              MaterialRepository materialRepository,
                              FacturaRepository facturaRepository,
                              PresupuestoPdfService presupuestoPdfService,
                              EmailService emailService,
                              PresupuestoCondicionesService presupuestoCondicionesService,
                              UsuarioRepository usuarioRepository) {
        this.presupuestoRepository = presupuestoRepository;
        this.clienteRepository = clienteRepository;
        this.empresaRepository = empresaRepository;
        this.materialRepository = materialRepository;
        this.facturaRepository = facturaRepository;
        this.presupuestoPdfService = presupuestoPdfService;
        this.emailService = emailService;
        this.presupuestoCondicionesService = presupuestoCondicionesService;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<String> obtenerMisCondicionesPredeterminadas(Long usuarioId) {
        Usuario u = usuarioRepository.findById(usuarioId).orElseThrow();
        return presupuestoCondicionesService.desdeJson(u.getCondicionesPresupuestoPredeterminadas());
    }

    @Transactional
    public void guardarMisCondicionesPredeterminadas(Long usuarioId, List<String> claves) {
        Usuario u = usuarioRepository.findById(usuarioId).orElseThrow();
        u.setCondicionesPresupuestoPredeterminadas(presupuestoCondicionesService.aJson(
                presupuestoCondicionesService.normalizarClaves(claves != null ? claves : List.of())));
        usuarioRepository.save(u);
    }

    @Transactional(readOnly = true)
    public List<PresupuestoResponse> listar(Long usuarioId, String q) {
        var stream = presupuestoRepository.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId).stream()
                .map(this::toResponse);
        if (q != null && !q.isBlank()) {
            String lower = q.strip().toLowerCase();
            stream = stream.filter(p ->
                    (p.clienteNombre() != null && p.clienteNombre().toLowerCase().contains(lower)) ||
                    (p.estado() != null && p.estado().toLowerCase().contains(lower))
            );
        }
        return stream.toList();
    }

    /**
     * Creación: {@code condicionesActivas == null} en JSON → se aplican predeterminadas del perfil;
     * lista vacía o con ítems → se respeta tal cual (sin mezclar con perfil).
     */
    private void aplicarCondicionesYNotaCrear(Presupuesto presupuesto, PresupuestoRequest request, Long usuarioId) {
        List<String> claves;
        if (request.condicionesActivas() == null) {
            Usuario u = usuarioRepository.findById(usuarioId).orElse(null);
            claves = u != null
                    ? presupuestoCondicionesService.desdeJson(u.getCondicionesPresupuestoPredeterminadas())
                    : List.of();
        } else {
            claves = presupuestoCondicionesService.normalizarClaves(request.condicionesActivas());
        }
        presupuesto.setCondicionesActivasJson(presupuestoCondicionesService.aJson(claves));
        String nota = request.notaAdicional();
        presupuesto.setNotaAdicional(nota != null && !nota.isBlank() ? nota.strip() : null);
    }

    private void aplicarCondicionesYNotaActualizar(Presupuesto presupuesto, PresupuestoRequest request) {
        List<String> claves = presupuestoCondicionesService.normalizarClaves(
                request.condicionesActivas() != null ? request.condicionesActivas() : List.of());
        presupuesto.setCondicionesActivasJson(presupuestoCondicionesService.aJson(claves));
        String nota = request.notaAdicional();
        presupuesto.setNotaAdicional(nota != null && !nota.isBlank() ? nota.strip() : null);
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
        aplicarCondicionesYNotaCrear(presupuesto, request, usuario.getId());

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
        aplicarCondicionesYNotaActualizar(presupuesto, request);

        presupuesto.getItems().clear();
        mapItems(request.items(), presupuesto);
        calcularTotales(presupuesto);
        presupuesto = presupuestoRepository.save(presupuesto);
        return toResponse(presupuesto);
    }

    @Transactional
    public PresupuestoResponse actualizarEstado(Long id, PresupuestoEstadoPatchRequest request, Long usuarioId) {
        Presupuesto presupuesto = presupuestoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Presupuesto no encontrado"));
        presupuesto.setEstado(normalizarEstadoPresupuesto(request.estado()));
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

    private static String normalizarEstadoPresupuesto(String estado) {
        String v = estado != null ? estado.trim() : "";
        if (PresupuestoEstado.PENDIENTE.equalsIgnoreCase(v)) {
            return PresupuestoEstado.PENDIENTE;
        }
        if (PresupuestoEstado.ACEPTADO.equalsIgnoreCase(v)) {
            return PresupuestoEstado.ACEPTADO;
        }
        if (PresupuestoEstado.RECHAZADO.equalsIgnoreCase(v)) {
            return PresupuestoEstado.RECHAZADO;
        }
        if (PresupuestoEstado.EN_EJECUCION.equalsIgnoreCase(v)
                || PresupuestoEstado.EN_EJECUCION_SIN_TILDE.equalsIgnoreCase(v)) {
            return PresupuestoEstado.EN_EJECUCION;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado de presupuesto no válido");
    }

    @Transactional(readOnly = true)
    public byte[] generarPdf(Long id, Long usuarioId) {
        Presupuesto presupuesto = presupuestoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Presupuesto no encontrado"));
        return presupuestoPdfService.generarPdf(presupuesto, usuarioId);
    }

    /** No usar readOnly: encola fila en {@code email_jobs} (escritura). */
    @Transactional
    public void enviarPorEmail(Long id, Long usuarioId, EnviarEmailRequest request) {
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
        String nombreEmpresa = empresaRepository.findByUsuarioId(usuarioId).map(e -> e.getNombre()).orElse(null);
        String nombreCliente = presupuesto.getCliente() != null ? presupuesto.getCliente().getNombre() : null;
        String cuerpo = EmailCopy.prefijoClienteEmpresa(nombreCliente, nombreEmpresa)
                + "<p>Adjunto encontrará el presupuesto solicitado.</p><p>Saludos cordiales.</p>";
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

        Long usuarioId = Objects.requireNonNull(presupuesto.getUsuario()).getId();
        Long facturaId = facturaRepository.findVentasPrincipalesPorPresupuesto(
                        presupuesto.getId(), usuarioId, TipoFactura.NORMAL, TipoFactura.FINAL_CON_ANTICIPO)
                .stream()
                .findFirst()
                .map(f -> f.getId())
                .orElse(null);

        var cli = presupuesto.getCliente();
        String estadoCliente = cli != null && cli.getEstadoCliente() != null
                ? cli.getEstadoCliente().name()
                : null;
        return new PresupuestoResponse(
                presupuesto.getId(),
                Objects.requireNonNull(Objects.requireNonNull(cli).getId()),
                cli.getNombre(),
                estadoCliente,
                cli.getEmail(),
                presupuesto.getFechaCreacion(),
                presupuesto.getSubtotal(),
                presupuesto.getIva(),
                presupuesto.getTotal(),
                presupuesto.getIvaHabilitado(),
                presupuesto.getEstado(),
                Optional.ofNullable(presupuesto.getDescuentoGlobalPorcentaje()).orElse(0.0),
                Optional.ofNullable(presupuesto.getDescuentoGlobalFijo()).orElse(0.0),
                Optional.ofNullable(presupuesto.getDescuentoAntesIva()).orElse(true),
                items,
                presupuestoCondicionesService.desdeJson(presupuesto.getCondicionesActivasJson()),
                presupuesto.getNotaAdicional(),
                Boolean.TRUE.equals(presupuesto.getTieneAnticipo()),
                presupuesto.getImporteAnticipo(),
                Boolean.TRUE.equals(presupuesto.getAnticipoFacturado()),
                presupuesto.getFechaAnticipo(),
                facturaId
        );
    }
}
