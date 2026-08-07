package com.appgestion.api.service;

import com.appgestion.api.constant.FacturaEstadoPago;
import com.appgestion.api.constant.PresupuestoEstado;
import com.appgestion.api.constant.TaxConstants;
import com.appgestion.api.dto.FacturaNumeroGenerado;
import com.appgestion.api.domain.entity.*;
import com.appgestion.api.domain.enums.TipoFactura;
import com.appgestion.api.service.factura.FacturaNumeroManualParser;
import com.appgestion.api.service.factura.FacturaNumeroResuelto;
import com.appgestion.api.dto.request.FacturaCobroRequest;
import com.appgestion.api.dto.request.FacturaItemRequest;
import com.appgestion.api.dto.request.FacturaRequest;
import com.appgestion.api.dto.request.EnviarEmailRequest;
import com.appgestion.api.dto.response.FacturaResponse;
import com.appgestion.api.repository.ClienteRepository;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.repository.FacturaCobroRepository;
import com.appgestion.api.repository.FacturaRepository;
import com.appgestion.api.repository.MaterialRepository;
import com.appgestion.api.repository.PresupuestoRepository;
import com.appgestion.api.util.NifValidator;
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

@Service
public class FacturaService {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final FacturaRepository facturaRepository;
    private final ClienteRepository clienteRepository;
    private final EmpresaRepository empresaRepository;
    private final PresupuestoRepository presupuestoRepository;
    private final MaterialRepository materialRepository;
    private final FacturaPdfService facturaPdfService;
    private final FacturaNumeroService facturaNumeroService;
    private final FacturaCobroRepository facturaCobroRepository;
    private final FacturaResponseMapper facturaResponseMapper;
    private final FacturaEmailService facturaEmailService;
    private final FacturaCobroService facturaCobroService;
    private final FacturaPaymentLinkService facturaPaymentLinkService;
    private final ClienteService clienteService;

    public FacturaService(FacturaRepository facturaRepository,
                          ClienteRepository clienteRepository,
                          EmpresaRepository empresaRepository,
                          PresupuestoRepository presupuestoRepository,
                          MaterialRepository materialRepository,
                          FacturaPdfService facturaPdfService,
                          FacturaNumeroService facturaNumeroService,
                          FacturaCobroRepository facturaCobroRepository,
                          FacturaResponseMapper facturaResponseMapper,
                          FacturaEmailService facturaEmailService,
                          FacturaCobroService facturaCobroService,
                          FacturaPaymentLinkService facturaPaymentLinkService,
                          ClienteService clienteService) {
        this.facturaRepository = facturaRepository;
        this.clienteRepository = clienteRepository;
        this.empresaRepository = empresaRepository;
        this.presupuestoRepository = presupuestoRepository;
        this.materialRepository = materialRepository;
        this.facturaPdfService = facturaPdfService;
        this.facturaNumeroService = facturaNumeroService;
        this.facturaCobroRepository = facturaCobroRepository;
        this.facturaResponseMapper = facturaResponseMapper;
        this.facturaEmailService = facturaEmailService;
        this.facturaCobroService = facturaCobroService;
        this.facturaPaymentLinkService = facturaPaymentLinkService;
        this.clienteService = clienteService;
    }

    @Transactional(readOnly = true)
    public List<FacturaResponse> listar(Long usuarioId, String q, boolean incluirAnuladas) {
        var stream = facturaRepository.findByUsuarioIdForList(usuarioId, incluirAnuladas).stream()
                .map(f -> facturaResponseMapper.toResponse(f, List.of()));
        if (q != null && !q.isBlank()) {
            String lower = q.strip().toLowerCase();
            stream = stream.filter(f ->
                    (f.numeroFactura() != null && f.numeroFactura().toLowerCase().contains(lower)) ||
                    (f.clienteNombre() != null && f.clienteNombre().toLowerCase().contains(lower)) ||
                    (f.notas() != null && f.notas().toLowerCase().contains(lower))
            );
        }
        return stream.toList();
    }

    @Transactional(readOnly = true)
    public FacturaResponse obtenerPorId(Long id, Long usuarioId) {
        Factura factura = facturaRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada"));
        return facturaResponseMapper.toResponse(factura, facturaCobroRepository.findByFacturaIdOrderByFechaDescCreatedAtDesc(factura.getId()));
    }

    @Transactional
    public FacturaResponse crear(FacturaRequest request, Usuario usuario) {
        Cliente cliente = clienteRepository.findByIdAndUsuarioId(request.clienteId(), usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));

        Presupuesto presupuesto = null;
        if (request.presupuestoId() != null) {
            presupuesto = presupuestoRepository.findByIdAndUsuarioId(request.presupuestoId(), usuario.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Presupuesto no encontrado"));
            presupuesto.setEstado(PresupuestoEstado.ACEPTADO);
        }

        validarDatosFactura(usuario.getId(), cliente);

        LocalDate fechaExp = request.fechaExpedicion();
        LocalDate fechaVencimiento = request.fechaVencimiento();
        if (fechaVencimiento != null && fechaExp != null && fechaVencimiento.isBefore(fechaExp)) {
            throw new IllegalArgumentException(
                    "La fecha de vencimiento no puede ser "
                            + "anterior a la fecha de expedición.");
        }

        FacturaNumeroResuelto numero = resolverNumeroParaCreacion(request, usuario.getId(), fechaExp);

        Factura factura = new Factura();
        factura.setUsuario(usuario);
        factura.setNumeroFactura(numero.numeroFactura());
        factura.setAnioFactura(numero.anioFactura());
        factura.setNumeroSecuencial(numero.numeroSecuencial());
        factura.setCliente(cliente);
        factura.setPresupuesto(presupuesto);
        factura.setTipoFactura(TipoFactura.NORMAL);
        factura.setFechaExpedicion(request.fechaExpedicion());
        factura.setFechaVencimiento(request.fechaVencimiento());
        factura.setMetodoPago(request.metodoPago());
        factura.setEstadoPago(request.estadoPago());
        factura.setMontoCobrado(request.montoCobrado());
        factura.setNotas(request.notas());
        factura.setIvaHabilitado(request.ivaHabilitado());

        mapItems(request.items(), factura);
        calcularTotales(factura);
        factura = facturaRepository.save(factura);
        return facturaResponseMapper.toResponse(factura, facturaCobroRepository.findByFacturaIdOrderByFechaDescCreatedAtDesc(factura.getId()));
    }

    @Transactional
    public FacturaResponse actualizar(Long id, FacturaRequest request, Long usuarioId) {
        Factura factura = facturaRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada"));

        if (Boolean.TRUE.equals(factura.getAnulada())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede editar una factura anulada");
        }

        Cliente cliente = clienteRepository.findByIdAndUsuarioId(request.clienteId(), usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));

        Presupuesto presupuesto = null;
        if (request.presupuestoId() != null) {
            presupuesto = presupuestoRepository.findByIdAndUsuarioId(request.presupuestoId(), usuarioId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Presupuesto no encontrado"));
            presupuesto.setEstado(PresupuestoEstado.ACEPTADO);
        }

        validarDatosFactura(usuarioId, cliente);

        aplicarCambioNumeroEnActualizacion(factura, request, usuarioId);

        factura.setCliente(cliente);
        factura.setPresupuesto(presupuesto);
        factura.setFechaExpedicion(request.fechaExpedicion());
        factura.setFechaOperacion(request.fechaOperacion());
        factura.setRegimenFiscal(request.regimenFiscal());
        factura.setCondicionesPago(request.condicionesPago());
        factura.setFechaVencimiento(request.fechaVencimiento());
        factura.setMetodoPago(request.metodoPago());
        factura.setEstadoPago(request.estadoPago());
        factura.setMontoCobrado(request.montoCobrado());
        factura.setNotas(request.notas());
        factura.setIvaHabilitado(request.ivaHabilitado());

        factura.getItems().clear();
        mapItems(request.items(), factura);
        calcularTotales(factura);
        factura = facturaRepository.save(factura);
        return facturaResponseMapper.toResponse(factura, facturaCobroRepository.findByFacturaIdOrderByFechaDescCreatedAtDesc(factura.getId()));
    }

    @Transactional
    public FacturaResponse crearDesdePresupuesto(Long presupuestoId, Usuario usuario) {
        Presupuesto presupuesto = presupuestoRepository.findByIdAndUsuarioId(presupuestoId, usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Presupuesto no encontrado"));

        validarReglasNegocioPresupuestoParaFacturaNormal(presupuesto, presupuestoId, usuario.getId());

        validarDatosFactura(usuario.getId(), presupuesto.getCliente());

        LocalDate fechaExp = LocalDate.now();
        FacturaNumeroGenerado gen = facturaNumeroService.generarSiguienteNumeroFactura(usuario.getId(), fechaExp.getYear());

        Factura factura = new Factura();
        factura.setUsuario(usuario);
        factura.setNumeroFactura(gen.numeroFactura());
        factura.setAnioFactura(gen.anioFactura());
        factura.setNumeroSecuencial(gen.numeroSecuencial());
        factura.setCliente(presupuesto.getCliente());
        factura.setPresupuesto(presupuesto);
        factura.setTipoFactura(TipoFactura.NORMAL);
        factura.setFechaExpedicion(fechaExp);
        factura.setRegimenFiscal("Régimen general del IVA");
        factura.setMoneda("EUR");
        factura.setIvaHabilitado(presupuesto.getIvaHabilitado());
        factura.setMetodoPago("Transferencia");
        factura.setEstadoPago(FacturaEstadoPago.NO_PAGADA);

        copiarItemsDesdePresupuesto(presupuesto, factura);

        presupuesto.setEstado(PresupuestoEstado.ACEPTADO);
        calcularTotales(factura);
        factura = facturaRepository.save(factura);
        return facturaResponseMapper.toResponse(factura, facturaCobroRepository.findByFacturaIdOrderByFechaDescCreatedAtDesc(factura.getId()));
    }

    @Transactional(readOnly = true)
    public byte[] generarPdf(Long id, Long usuarioId) {
        Factura factura = facturaRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada"));
        return facturaPdfService.generarPdf(factura, usuarioId);
    }

    @Transactional(readOnly = true)
    public void enviarPorEmail(Long id, Long usuarioId, EnviarEmailRequest request) {
        facturaEmailService.enviarPorEmail(id, usuarioId, request);
    }

    @Transactional
    public void anular(Long id, Long usuarioId, String motivo) {
        Factura factura = facturaRepository.findByIdAndUsuarioId(Objects.requireNonNull(id), Objects.requireNonNull(usuarioId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada"));
        if (Boolean.TRUE.equals(factura.getAnulada())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La factura ya está anulada");
        }
        factura.setAnulada(true);
        factura.setFechaAnulacion(LocalDate.now());
        factura.setMotivoAnulacion(motivo != null && !motivo.isBlank() ? motivo.trim() : null);
        facturaRepository.save(factura);
    }

    @Transactional
    public FacturaResponse registrarCobro(Long facturaId, Long usuarioId, FacturaCobroRequest request) {
        return facturaCobroService.registrarCobro(facturaId, usuarioId, request);
    }

    @Transactional
    public FacturaResponse generarPaymentLink(Long facturaId, Long usuarioId) {
        return facturaPaymentLinkService.generarPaymentLink(facturaId, usuarioId);
    }

    private FacturaNumeroResuelto resolverNumeroParaCreacion(FacturaRequest request, Long usuarioId, LocalDate fechaExpedicion) {
        int anioExp = fechaExpedicion.getYear();
        if (request.numeroFactura() == null || request.numeroFactura().isBlank()) {
            FacturaNumeroGenerado gen = facturaNumeroService.generarSiguienteNumeroFactura(usuarioId, anioExp);
            return new FacturaNumeroResuelto(gen.numeroFactura(), gen.anioFactura(), gen.numeroSecuencial());
        }
        String numeroFactura = request.numeroFactura().trim();
        if (facturaRepository.findByNumeroFacturaAndUsuarioId(numeroFactura, usuarioId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe una factura con ese número");
        }
        FacturaNumeroManualParser.ParsedFac p = FacturaNumeroManualParser.parseFacManual(numeroFactura);
        if (p == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El número manual debe seguir el formato FAC-AAAA-NNNN (o deje el número vacío para asignación automática).");
        }
        if (p.anio() != anioExp) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El año del número de factura debe coincidir con el año de la fecha de expedición.");
        }
        if (facturaRepository.existsByUsuario_IdAndAnioFacturaAndNumeroSecuencial(usuarioId, p.anio(), p.secuencial())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe una factura con ese número en la serie.");
        }
        return new FacturaNumeroResuelto(numeroFactura, p.anio(), p.secuencial());
    }

    private void aplicarCambioNumeroEnActualizacion(Factura factura, FacturaRequest request, Long usuarioId) {
        if (request.numeroFactura() == null || request.numeroFactura().isBlank()
                || request.numeroFactura().equals(factura.getNumeroFactura())) {
            return;
        }
        if (facturaRepository.findByNumeroFacturaAndUsuarioId(request.numeroFactura(), usuarioId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe una factura con ese número");
        }
        String nuevo = request.numeroFactura().trim();
        FacturaNumeroManualParser.ParsedFac p = FacturaNumeroManualParser.parseFacManual(nuevo);
        if (p == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El número manual debe seguir el formato FAC-AAAA-NNNN.");
        }
        int anioExp = request.fechaExpedicion().getYear();
        if (p.anio() != anioExp) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El año del número de factura debe coincidir con el año de la fecha de expedición.");
        }
        if (facturaRepository.existsByUsuario_IdAndAnioFacturaAndNumeroSecuencialAndIdNot(
                usuarioId, p.anio(), p.secuencial(), factura.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe una factura con ese número en la serie.");
        }
        factura.setNumeroFactura(nuevo);
        factura.setAnioFactura(p.anio());
        factura.setNumeroSecuencial(p.secuencial());
    }

    private void validarReglasNegocioPresupuestoParaFacturaNormal(Presupuesto presupuesto, Long presupuestoId, Long usuarioId) {
        if (Boolean.TRUE.equals(presupuesto.getTieneAnticipo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Este presupuesto usa anticipo fiscal. Genera la factura final con POST /presupuestos/{id}/factura-final.");
        }
        if (!facturaRepository.findVentasPrincipalesPorPresupuesto(
                presupuestoId, usuarioId, TipoFactura.NORMAL, TipoFactura.FINAL_CON_ANTICIPO).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este presupuesto ya tiene una factura asociada");
        }
        String estado = presupuesto.getEstado() != null ? presupuesto.getEstado().trim() : "";
        if (PresupuestoEstado.RECHAZADO.equalsIgnoreCase(estado)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede facturar un presupuesto rechazado");
        }
        if (!PresupuestoEstado.PENDIENTE.equalsIgnoreCase(estado)
                && !PresupuestoEstado.ACEPTADO.equalsIgnoreCase(estado)
                && !PresupuestoEstado.EN_EJECUCION.equalsIgnoreCase(estado)
                && !PresupuestoEstado.EN_EJECUCION_SIN_TILDE.equalsIgnoreCase(estado)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Solo se puede facturar un presupuesto pendiente, aceptado o en ejecución");
        }
    }

    private void copiarItemsDesdePresupuesto(Presupuesto presupuesto, Factura factura) {
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
    }

    private void validarDatosFactura(Long usuarioId, Cliente cliente) {
        clienteService.validarClienteParaFactura(cliente.getId(), usuarioId);

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

        // Cliente COMPLETO: DNI, dirección y CP obligatorios; provincia/país opcionales en alta rápida.
        if (cliente.getCodigoPostal() == null || cliente.getCodigoPostal().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código postal del cliente es obligatorio para facturación");
        }
        String nifCliente = cliente.getDni();
        if (nifCliente == null || nifCliente.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El NIF del cliente es obligatorio para facturación");
        }
        if (!NifValidator.esValido(nifCliente)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El NIF del cliente no es válido");
        }
        String nifEmisor = empresa.getNif();
        if (nifEmisor != null && !nifEmisor.isBlank()
                && nifEmisor.trim().equalsIgnoreCase(nifCliente.trim())) {
            throw new IllegalArgumentException("El NIF del emisor y del destinatario no pueden ser el mismo.");
        }
        if (cliente.getDireccion() == null || cliente.getDireccion().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La dirección del cliente es obligatoria para facturación");
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
                Long uid = Objects.requireNonNull(factura.getUsuario()).getId();
                materialRepository.findByIdAndUsuarioId(Objects.requireNonNull(req.materialId()), uid).ifPresent(item::setMaterial);
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
                ? baseIva.multiply(TaxConstants.IVA_RATE).setScale(SCALE, ROUNDING)
                : BigDecimal.ZERO;
        factura.setIva(cuotaIvaTotal.doubleValue());
        BigDecimal bruto = subtotal.add(cuotaIvaTotal).setScale(SCALE, ROUNDING);
        factura.setTotal(bruto.doubleValue());

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

    /** Recalcula subtotal, IVA y total. En facturas finales con anticipo, las líneas ya representan el remanente. */
    public void recalcularTotales(Factura factura) {
        calcularTotales(factura);
    }

    /** Validación de datos fiscales mínimos para emitir factura (reutilizado por anticipos). */
    public void validarEmisionFactura(Long usuarioId, Cliente cliente) {
        validarDatosFactura(usuarioId, cliente);
    }
}
