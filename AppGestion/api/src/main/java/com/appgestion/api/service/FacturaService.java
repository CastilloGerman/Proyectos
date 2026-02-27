package com.appgestion.api.service;

import com.appgestion.api.domain.entity.*;
import com.appgestion.api.dto.request.FacturaItemRequest;
import com.appgestion.api.dto.request.FacturaRequest;
import com.appgestion.api.dto.request.EnviarEmailRequest;
import com.appgestion.api.dto.response.FacturaItemResponse;
import com.appgestion.api.dto.response.FacturaResponse;
import com.appgestion.api.repository.ClienteRepository;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.repository.FacturaRepository;
import com.appgestion.api.repository.MaterialRepository;
import com.appgestion.api.repository.PresupuestoRepository;
import com.appgestion.api.util.NifValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.mail.MessagingException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class FacturaService {

    private static final BigDecimal IVA_RATE = new BigDecimal("0.21");
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final FacturaRepository facturaRepository;
    private final ClienteRepository clienteRepository;
    private final EmpresaRepository empresaRepository;
    private final PresupuestoRepository presupuestoRepository;
    private final MaterialRepository materialRepository;
    private final FacturaPdfService facturaPdfService;
    private final FacturaNumeroService facturaNumeroService;
    private final EmailService emailService;

    public FacturaService(FacturaRepository facturaRepository,
                          ClienteRepository clienteRepository,
                          EmpresaRepository empresaRepository,
                          PresupuestoRepository presupuestoRepository,
                          MaterialRepository materialRepository,
                          FacturaPdfService facturaPdfService,
                          FacturaNumeroService facturaNumeroService,
                          EmailService emailService) {
        this.facturaRepository = facturaRepository;
        this.clienteRepository = clienteRepository;
        this.empresaRepository = empresaRepository;
        this.presupuestoRepository = presupuestoRepository;
        this.materialRepository = materialRepository;
        this.facturaPdfService = facturaPdfService;
        this.facturaNumeroService = facturaNumeroService;
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

        validarDatosFactura(usuario.getId(), cliente);

        String numeroFactura = request.numeroFactura();
        if (numeroFactura == null || numeroFactura.isBlank()) {
            numeroFactura = facturaNumeroService.generarSiguienteNumero(usuario.getId());
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

        validarDatosFactura(usuarioId, cliente);

        if (request.numeroFactura() != null && !request.numeroFactura().isBlank() && !request.numeroFactura().equals(factura.getNumeroFactura())) {
            if (facturaRepository.findByNumeroFacturaAndUsuarioId(request.numeroFactura(), usuarioId).isPresent()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe una factura con ese número");
            }
            factura.setNumeroFactura(request.numeroFactura());
        }

        factura.setCliente(cliente);
        factura.setPresupuesto(presupuesto);
        factura.setFechaExpedicion(request.fechaExpedicion() != null ? request.fechaExpedicion() : (factura.getFechaExpedicion() != null ? factura.getFechaExpedicion() : LocalDate.now()));
        factura.setFechaOperacion(request.fechaOperacion());
        factura.setRegimenFiscal(request.regimenFiscal());
        factura.setCondicionesPago(request.condicionesPago());
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

        validarDatosFactura(usuario.getId(), presupuesto.getCliente());

        String numeroFactura = facturaNumeroService.generarSiguienteNumero(usuario.getId());

        Factura factura = new Factura();
        factura.setUsuario(usuario);
        factura.setNumeroFactura(numeroFactura);
        factura.setCliente(presupuesto.getCliente());
        factura.setPresupuesto(presupuesto);
        factura.setFechaExpedicion(LocalDate.now());
        factura.setRegimenFiscal("Régimen general del IVA");
        factura.setMoneda("EUR");
        factura.setIvaHabilitado(presupuesto.getIvaHabilitado());
        factura.setMetodoPago("Transferencia");
        factura.setEstadoPago("No Pagada");

        for (PresupuestoItem pi : presupuesto.getItems()) {
            FacturaItem item = new FacturaItem();
            item.setFactura(factura);
            item.setMaterial(pi.getMaterial());
            item.setTareaManual(pi.getTareaManual());
            item.setEsTareaManual(Optional.ofNullable(pi.getEsTareaManual()).orElse(false));
            BigDecimal cant = BigDecimal.valueOf(Optional.ofNullable(pi.getCantidad()).orElse(0.0));
            BigDecimal precio = BigDecimal.valueOf(Optional.ofNullable(pi.getPrecioUnitario()).orElse(0.0));
            item.setCantidad(cant.doubleValue());
            item.setPrecioUnitario(precio.doubleValue());
            BigDecimal st = cant.multiply(precio).setScale(SCALE, ROUNDING);
            item.setSubtotal(st.doubleValue());
            item.setCuotaIva(0.0);
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
        emailService.enviarPdf(usuarioId, email, asunto, cuerpo, pdf, nombreArchivo);
    }

    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        if (!facturaRepository.existsByIdAndUsuarioId(Objects.requireNonNull(id), Objects.requireNonNull(usuarioId))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada");
        }
        facturaRepository.deleteById(Objects.requireNonNull(id));
    }

    private void validarDatosFactura(Long usuarioId, Cliente cliente) {
        Empresa empresa = empresaRepository.findByUsuarioId(usuarioId).orElse(null);
        if (empresa == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe configurar los datos de la empresa antes de emitir facturas");
        }
        if (empresa.getCodigoPostal() == null || empresa.getCodigoPostal().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código postal de la empresa es obligatorio para facturación");
        }
        if (empresa.getProvincia() == null || empresa.getProvincia().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La provincia de la empresa es obligatoria para facturación");
        }
        if (empresa.getPais() == null || empresa.getPais().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El país de la empresa es obligatorio para facturación");
        }
        if (empresa.getNif() != null && !empresa.getNif().isBlank() && !NifValidator.esValido(empresa.getNif())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El NIF de la empresa no es válido");
        }

        if (cliente.getCodigoPostal() == null || cliente.getCodigoPostal().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código postal del cliente es obligatorio para facturación");
        }
        if (cliente.getProvincia() == null || cliente.getProvincia().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La provincia del cliente es obligatoria para facturación");
        }
        if (cliente.getPais() == null || cliente.getPais().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El país del cliente es obligatorio para facturación");
        }
        String nifCliente = cliente.getDni();
        if (nifCliente != null && !nifCliente.isBlank() && !NifValidator.esValido(nifCliente)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El NIF del cliente no es válido");
        }
    }

    private void mapItems(List<FacturaItemRequest> itemRequests, Factura factura) {
        for (FacturaItemRequest req : itemRequests) {
            FacturaItem item = new FacturaItem();
            item.setFactura(factura);
            BigDecimal cantidad = BigDecimal.valueOf(Optional.ofNullable(req.cantidad()).orElse(0.0));
            BigDecimal precioUnitario = BigDecimal.valueOf(Optional.ofNullable(req.precioUnitario()).orElse(0.0));
            item.setCantidad(cantidad.doubleValue());
            item.setPrecioUnitario(precioUnitario.doubleValue());
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

            BigDecimal itemSubtotal = cantidad.multiply(precioUnitario).setScale(SCALE, ROUNDING);
            item.setSubtotal(itemSubtotal.doubleValue());
            item.setCuotaIva(0.0);

            factura.getItems().add(item);
        }
    }

    private void calcularTotales(Factura factura) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal baseIva = BigDecimal.ZERO;

        for (FacturaItem item : factura.getItems()) {
            BigDecimal itemSubtotal = BigDecimal.valueOf(item.getSubtotal()).setScale(SCALE, ROUNDING);
            subtotal = subtotal.add(itemSubtotal);
            if (Boolean.TRUE.equals(item.getAplicaIva())) {
                baseIva = baseIva.add(itemSubtotal);
            }
        }

        factura.setSubtotal(subtotal.setScale(SCALE, ROUNDING).doubleValue());

        BigDecimal cuotaIvaTotal = Boolean.TRUE.equals(factura.getIvaHabilitado())
                ? baseIva.multiply(IVA_RATE).setScale(SCALE, ROUNDING)
                : BigDecimal.ZERO;
        factura.setIva(cuotaIvaTotal.doubleValue());
        factura.setTotal(subtotal.add(cuotaIvaTotal).setScale(SCALE, ROUNDING).doubleValue());

        if (Boolean.TRUE.equals(factura.getIvaHabilitado()) && baseIva.compareTo(BigDecimal.ZERO) > 0) {
            for (FacturaItem item : factura.getItems()) {
                if (Boolean.TRUE.equals(item.getAplicaIva())) {
                    BigDecimal proporcion = BigDecimal.valueOf(item.getSubtotal()).divide(baseIva, 10, ROUNDING);
                    BigDecimal cuotaIva = cuotaIvaTotal.multiply(proporcion).setScale(SCALE, ROUNDING);
                    item.setCuotaIva(cuotaIva.doubleValue());
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
                        item.getSubtotal(),
                        Boolean.TRUE.equals(item.getAplicaIva()) || item.getAplicaIva() == null
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
                factura.getFechaExpedicion(),
                factura.getFechaOperacion(),
                factura.getFechaVencimiento(),
                factura.getSubtotal(),
                factura.getIva(),
                factura.getTotal(),
                factura.getIvaHabilitado(),
                factura.getRegimenFiscal(),
                factura.getCondicionesPago(),
                factura.getMetodoPago(),
                factura.getEstadoPago(),
                factura.getNotas(),
                items
        );
    }
}
