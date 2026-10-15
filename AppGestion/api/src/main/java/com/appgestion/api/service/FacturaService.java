package com.appgestion.api.service;

import com.appgestion.api.constant.FacturaEstadoPago;
import com.appgestion.api.constant.PresupuestoEstado;
import com.appgestion.api.fiscal.CalculoFactura;
import com.appgestion.api.fiscal.RepartoDescuento;
import com.appgestion.api.fiscal.TipoIva;
import com.appgestion.api.dto.FacturaNumeroGenerado;
import com.appgestion.api.domain.entity.*;
import com.appgestion.api.domain.enums.TipoFactura;
import com.appgestion.api.service.factura.FacturaNumeroManualParser;
import com.appgestion.api.service.factura.FacturaNumeroResuelto;
import com.appgestion.api.dto.request.EstadoPagoFacturaRequest;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class FacturaService {

    private static final int SCALE = 2;
    /** Cantidad y precio de la factura final con anticipo se guardan a 4 decimales. */
    private static final int ESCALA_CANTIDAD_PRECIO = 4;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    static final String AVISO_DESCUENTO_PERDIDO = "se ha perdido el descuento de esta línea";

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
    private final FacturaAuditoriaService facturaAuditoriaService;

    public FacturaService(FacturaRepository facturaRepository,
                          ClienteRepository clienteRepository,
                          EmpresaRepository empresaRepository,
                          PresupuestoRepository presupuestoRepository,
                          MaterialRepository materialRepository,
                          FacturaPdfService facturaPdfService,
                          FacturaNumeroService facturaNumeroService,
                          FacturaCobroRepository facturaCobroRepository,
                          FacturaResponseMapper facturaResponseMapper,
                          @Lazy FacturaEmailService facturaEmailService,
                          FacturaCobroService facturaCobroService,
                          FacturaPaymentLinkService facturaPaymentLinkService,
                          ClienteService clienteService,
                          FacturaAuditoriaService facturaAuditoriaService) {
        this.facturaAuditoriaService = facturaAuditoriaService;
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
        List<Long> ids = facturaRepository.findIdsByUsuarioIdForList(usuarioId, incluirAnuladas);
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, Factura> byId = new LinkedHashMap<>();
        for (Factura factura : facturaRepository.findForListByIds(ids)) {
            byId.putIfAbsent(factura.getId(), factura);
        }
        var stream = ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(facturaResponseMapper::toListResponse);
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
        factura.setEstadoPago(normalizarEstadoPago(request.estadoPago())); // SEC-16: solo valores del catálogo
        factura.setMontoCobrado(request.montoCobrado());
        factura.setNotas(request.notas());
        factura.setIvaHabilitado(request.ivaHabilitado());

        mapItems(request.items(), factura);
        calcularTotales(factura);
        factura = facturaRepository.save(factura);
        facturaAuditoriaService.registrar(factura, FacturaAuditoriaService.EVENTO_CREADA, null, usuario.getId());
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

        // SEC-15: instantánea previa a cualquier cambio, para la pista de auditoría.
        Map<String, Object> antes = facturaAuditoriaService.snapshot(factura);

        aplicarCambioNumeroEnActualizacion(factura, request, usuarioId);

        factura.setCliente(cliente);
        factura.setPresupuesto(presupuesto);
        factura.setFechaExpedicion(request.fechaExpedicion());
        factura.setFechaOperacion(request.fechaOperacion());
        factura.setRegimenFiscal(request.regimenFiscal());
        factura.setCondicionesPago(request.condicionesPago());
        factura.setFechaVencimiento(request.fechaVencimiento());
        factura.setMetodoPago(request.metodoPago());
        factura.setEstadoPago(normalizarEstadoPago(request.estadoPago())); // SEC-16: solo valores del catálogo
        factura.setMontoCobrado(request.montoCobrado());
        factura.setNotas(request.notas());
        factura.setIvaHabilitado(request.ivaHabilitado());

        List<LineaGuardada> anteriores = copiarLineas(factura);
        factura.getItems().clear();
        List<String> avisos = new ArrayList<>();
        mapItems(request.items(), factura, anteriores, avisos);
        calcularTotales(factura);
        factura = facturaRepository.save(factura);
        facturaAuditoriaService.registrar(factura, FacturaAuditoriaService.EVENTO_EDITADA, antes, usuarioId);
        return facturaResponseMapper.toResponse(
                factura,
                facturaCobroRepository.findByFacturaIdOrderByFechaDescCreatedAtDesc(factura.getId()),
                avisos);
    }

    /**
     * Estado de pago desde el listado. No sustituye líneas ni llama a {@link #calcularTotales}:
     * subtotal, IVA y total se quedan como están. En Pagada o Parcial actualiza la fecha del
     * último cobro (o crea uno si no había) para el criterio por fecha de cobro.
     * <p>
     * Ese criterio imputa la factura pagada al trimestre de {@code MAX(factura_cobros.fecha)}.
     * Este método no pasa por el aviso de confirmación previsto para cuando cambien importes,
     * líneas, tipos o fechas de una factura cuyo trimestre ya venció: el cobro queda fuera de
     * esa puerta, y el aviso no está implementado. Consecuencias, sin bloquearlas:
     * <ul>
     *   <li>Si llega {@code fechaCobro}, se mueve la fecha del último cobro y el IVA puede salir
     *       de un trimestre ya vencido y entrar en otro.</li>
     *   <li>El listado no envía fecha. Si no había cobros, se crea uno con la fecha de hoy, que
     *       cae en el trimestre en curso y no reescribe uno ya vencido. Si ya había cobros y no
     *       llega fecha, la fecha no se toca.</li>
     *   <li>Pasar de Pagada a No Pagada o Parcial saca la factura del trimestre de su último cobro
     *       aunque ese plazo ya haya vencido, porque «Por fecha de cobro» solo suma Pagada.</li>
     * </ul>
     * Registrar un cobro desde el formulario mueve el mismo trimestre y tampoco pasa por el aviso.
     */
    @Transactional
    public FacturaResponse actualizarEstadoPago(Long id, Long usuarioId, EstadoPagoFacturaRequest request) {
        Factura factura = facturaRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada"));
        if (Boolean.TRUE.equals(factura.getAnulada())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede editar una factura anulada");
        }

        Map<String, Object> antes = facturaAuditoriaService.snapshot(factura);
        String estado = normalizarEstadoPago(request.estadoPago());
        factura.setEstadoPago(estado);

        List<FacturaCobro> cobros = new java.util.ArrayList<>(
                facturaCobroRepository.findByFacturaIdOrderByFechaDescCreatedAtDesc(factura.getId()));
        if (FacturaEstadoPago.PARCIAL.equals(estado)) {
            if (request.montoCobrado() == null || request.montoCobrado() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica el importe cobrado");
            }
            factura.setMontoCobrado(request.montoCobrado());
            aplicarFechaCobro(factura, cobros, request.fechaCobro(), request.montoCobrado());
        } else if (FacturaEstadoPago.PAGADA.equals(estado)) {
            double totalGuardado = factura.getTotal() == null ? 0.0 : factura.getTotal();
            boolean cobroNuevo = aplicarFechaCobro(factura, cobros, request.fechaCobro(), totalGuardado);
            if (cobroNuevo) {
                factura.setMontoCobrado(totalGuardado);
            }
        }

        facturaRepository.save(factura);
        facturaAuditoriaService.registrar(factura, FacturaAuditoriaService.EVENTO_ESTADO_PAGO, antes, usuarioId);
        return facturaResponseMapper.toResponse(factura, cobros);
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
        facturaAuditoriaService.registrar(factura, FacturaAuditoriaService.EVENTO_CREADA, null, usuario.getId());
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
        Map<String, Object> antes = facturaAuditoriaService.snapshot(factura);
        factura.setAnulada(true);
        factura.setFechaAnulacion(LocalDate.now());
        factura.setMotivoAnulacion(motivo != null && !motivo.isBlank() ? motivo.trim() : null);
        facturaRepository.save(factura);
        facturaAuditoriaService.registrar(factura, FacturaAuditoriaService.EVENTO_ANULADA, antes, usuarioId);
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

    /**
     * La base de cada línea es el subtotal del presupuesto, ya con el descuento de
     * línea. Si el descuento global es anterior al IVA, se reparte sobre esas bases.
     * El precio unitario pasa a ser la base neta solo cuando cantidad × precio cuadra
     * a céntimo; si no, se conserva el precio original.
     * <p>
     * El descuento posterior al IVA no se resta de la factura: pendiente de decisión
     * fiscal. La base repartida ya va a céntimo. {@link #calcularTotales(Factura)}
     * calcula la cuota sobre esa base y el total es base + cuota (opción B, a la
     * espera de que el gestor la confirme). El presupuesto redondea la suma sin
     * redondear, así que su total puede quedar 0,01 € por debajo.
     */
    private void copiarItemsDesdePresupuesto(Presupuesto presupuesto, Factura factura) {
        List<PresupuestoItem> origen = presupuesto.getItems();
        List<RepartoDescuento.Linea> lineas = new ArrayList<>();
        for (PresupuestoItem pi : origen) {
            boolean aplica = !Boolean.FALSE.equals(pi.getAplicaIva());
            lineas.add(new RepartoDescuento.Linea(
                    dinero(pi.getSubtotal()),
                    aplica ? TipoIva.GENERAL : TipoIva.CERO));
        }
        boolean antesDeIva = Boolean.TRUE.equals(presupuesto.getDescuentoAntesIva());
        // Posterior al IVA: no se aplica. Pendiente de decisión fiscal.
        BigDecimal porcentaje = antesDeIva ? dinero(presupuesto.getDescuentoGlobalPorcentaje()) : BigDecimal.ZERO;
        BigDecimal fijo = antesDeIva ? dinero(presupuesto.getDescuentoGlobalFijo()) : BigDecimal.ZERO;
        List<BigDecimal> bases = RepartoDescuento.repartir(lineas, porcentaje, fijo);

        for (int i = 0; i < origen.size(); i++) {
            PresupuestoItem pi = origen.get(i);
            FacturaItem item = new FacturaItem();
            item.setFactura(factura);
            item.setMaterial(pi.getMaterial());
            item.setTareaManual(pi.getTareaManual());
            item.setEsTareaManual(Optional.ofNullable(pi.getEsTareaManual()).orElse(false));
            BigDecimal cant = dinero(pi.getCantidad());
            BigDecimal precio = dinero(pi.getPrecioUnitario());
            BigDecimal neto = bases.get(i);
            item.setCantidad(cant.doubleValue());
            item.setPrecioUnitario(precioUnitarioDeLinea(cant, precio, neto).doubleValue());
            item.setSubtotal(neto.doubleValue());
            item.setCuotaIva(0.0);
            boolean aplica = !Boolean.FALSE.equals(pi.getAplicaIva());
            item.setAplicaIva(aplica);
            item.setTipoIva(aplica ? TipoIva.GENERAL : TipoIva.CERO);
            factura.getItems().add(item);
        }
    }

    private static BigDecimal dinero(Double value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        }
        return BigDecimal.valueOf(value);
    }

    private static BigDecimal precioUnitarioDeLinea(BigDecimal cantidad, BigDecimal precioOriginal, BigDecimal neto) {
        if (cantidad.signum() <= 0) {
            return precioOriginal;
        }
        BigDecimal unitario = neto.divide(cantidad, SCALE, ROUNDING);
        BigDecimal reconstruido = unitario.multiply(cantidad).setScale(SCALE, ROUNDING);
        if (reconstruido.compareTo(neto.setScale(SCALE, ROUNDING)) == 0) {
            return unitario;
        }
        return precioOriginal;
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
        mapItems(itemRequests, factura, List.of(), null);
    }

    /**
     * Al editar, la base neta se conserva si la línea se reconoce y no cambian
     * cantidad, precio ni tipo. El reconocimiento es por id. Sin id, por
     * descripción, cantidad, precio y tipo, y solo si esa coincidencia es única.
     * Cantidad y precio se comparan a {@link #ESCALA_CANTIDAD_PRECIO} decimales.
     */
    private void mapItems(
            List<FacturaItemRequest> itemRequests,
            Factura factura,
            List<LineaGuardada> anteriores,
            List<String> avisos) {
        Set<Long> usadosPorId = new HashSet<>();
        Set<Integer> usadosPorIndice = new HashSet<>();
        for (FacturaItemRequest req : itemRequests) {
            FacturaItem item = new FacturaItem();
            item.setFactura(factura);
            BigDecimal cantidad = BigDecimal.valueOf(Optional.ofNullable(req.cantidad()).orElse(0.0));
            BigDecimal precioUnitario = BigDecimal.valueOf(Optional.ofNullable(req.precioUnitario()).orElse(0.0));
            item.setCantidad(cantidad.doubleValue());
            item.setPrecioUnitario(precioUnitario.doubleValue());
            int tipo;
            try {
                tipo = TipoIva.resolver(req.tipoIva(), req.aplicaIva());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
            }
            item.setTipoIva(tipo);
            item.setAplicaIva(tipo != TipoIva.CERO);

            if (req.materialId() != null) {
                Long uid = Objects.requireNonNull(factura.getUsuario()).getId();
                materialRepository.findByIdAndUsuarioId(Objects.requireNonNull(req.materialId()), uid).ifPresent(item::setMaterial);
                item.setEsTareaManual(false);
                item.setTareaManual(null);
            } else {
                item.setEsTareaManual(true);
                item.setTareaManual(req.tareaManual());
            }

            LineaGuardada anterior = emparejar(req, tipo, anteriores, usadosPorId, usadosPorIndice);
            boolean mismaLinea = anterior != null
                    && mismaEscala(cantidad, anterior.cantidad())
                    && mismaEscala(precioUnitario, anterior.precio())
                    && tipo == anterior.tipo();
            if (mismaLinea) {
                item.setSubtotal(anterior.subtotal().doubleValue());
            } else {
                BigDecimal itemSubtotal = cantidad.multiply(precioUnitario).setScale(SCALE, ROUNDING);
                item.setSubtotal(itemSubtotal.doubleValue());
                if (avisos != null && anterior != null && baseDistintaDelProducto(anterior)) {
                    avisos.add(AVISO_DESCUENTO_PERDIDO);
                }
            }
            item.setCuotaIva(0.0);

            factura.getItems().add(item);
        }
    }

    private LineaGuardada emparejar(
            FacturaItemRequest req,
            int tipo,
            List<LineaGuardada> anteriores,
            Set<Long> usadosPorId,
            Set<Integer> usadosPorIndice) {
        if (req.id() != null) {
            for (int i = 0; i < anteriores.size(); i++) {
                LineaGuardada linea = anteriores.get(i);
                if (linea.id() != null && req.id().equals(linea.id()) && usadosPorId.add(linea.id())) {
                    usadosPorIndice.add(i);
                    return linea;
                }
            }
            return null;
        }
        List<Integer> coincidencias = new ArrayList<>();
        for (int i = 0; i < anteriores.size(); i++) {
            if (usadosPorIndice.contains(i)) {
                continue;
            }
            LineaGuardada linea = anteriores.get(i);
            if (mismaTupla(req, tipo, linea)) {
                coincidencias.add(i);
            }
        }
        if (coincidencias.size() != 1) {
            return null;
        }
        int indice = coincidencias.get(0);
        usadosPorIndice.add(indice);
        LineaGuardada linea = anteriores.get(indice);
        if (linea.id() != null) {
            usadosPorId.add(linea.id());
        }
        return linea;
    }

    private static boolean mismaTupla(FacturaItemRequest req, int tipo, LineaGuardada linea) {
        return texto(req.tareaManual()).equals(texto(linea.descripcion()))
                && mismaEscala(BigDecimal.valueOf(Optional.ofNullable(req.cantidad()).orElse(0.0)), linea.cantidad())
                && mismaEscala(BigDecimal.valueOf(Optional.ofNullable(req.precioUnitario()).orElse(0.0)), linea.precio())
                && tipo == linea.tipo();
    }

    private static boolean mismaEscala(BigDecimal pedido, BigDecimal guardado) {
        return pedido.setScale(ESCALA_CANTIDAD_PRECIO, ROUNDING)
                .compareTo(guardado.setScale(ESCALA_CANTIDAD_PRECIO, ROUNDING)) == 0;
    }

    private static boolean baseDistintaDelProducto(LineaGuardada linea) {
        BigDecimal producto = linea.cantidad().multiply(linea.precio()).setScale(SCALE, ROUNDING);
        return producto.compareTo(linea.subtotal().setScale(SCALE, ROUNDING)) != 0;
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private static List<LineaGuardada> copiarLineas(Factura factura) {
        List<LineaGuardada> copias = new ArrayList<>();
        for (FacturaItem item : factura.getItems()) {
            String descripcion = Boolean.TRUE.equals(item.getEsTareaManual())
                    ? item.getTareaManual()
                    : (item.getMaterial() != null ? item.getMaterial().getNombre() : "");
            copias.add(new LineaGuardada(
                    item.getId(),
                    descripcion,
                    dinero(item.getCantidad()),
                    dinero(item.getPrecioUnitario()),
                    item.getTipoIva() == null ? TipoIva.GENERAL : item.getTipoIva(),
                    dinero(item.getSubtotal())));
        }
        return copias;
    }

    private record LineaGuardada(
            Long id,
            String descripcion,
            BigDecimal cantidad,
            BigDecimal precio,
            int tipo,
            BigDecimal subtotal) {
    }

    /**
     * @return true si no había cobros y se ha creado uno. No modifica importes de cobros ya existentes:
     *         solo su fecha.
     */
    private boolean aplicarFechaCobro(Factura factura, List<FacturaCobro> cobros, LocalDate fechaPedida, double importeSiNuevo) {
        if (!cobros.isEmpty()) {
            if (fechaPedida != null) {
                FacturaCobro ultimo = cobros.get(0);
                ultimo.setFecha(fechaPedida);
                facturaCobroRepository.save(ultimo);
            }
            return false;
        }
        LocalDate fecha = fechaPedida != null ? fechaPedida : LocalDate.now();
        FacturaCobro cobro = new FacturaCobro();
        cobro.setFactura(factura);
        cobro.setImporte(importeSiNuevo);
        cobro.setFecha(fecha);
        cobro.setMetodo(factura.getMetodoPago());
        cobro.setNotas("Cambio de estado de pago");
        cobros.add(facturaCobroRepository.save(cobro));
        return true;
    }

    private static String normalizarEstadoPago(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado de pago es obligatorio");
        }
        String texto = raw.trim();
        if (texto.equalsIgnoreCase(FacturaEstadoPago.PAGADA)) {
            return FacturaEstadoPago.PAGADA;
        }
        if (texto.equalsIgnoreCase(FacturaEstadoPago.NO_PAGADA)) {
            return FacturaEstadoPago.NO_PAGADA;
        }
        if (texto.equalsIgnoreCase(FacturaEstadoPago.PARCIAL)) {
            return FacturaEstadoPago.PARCIAL;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado de pago no válido");
    }

    /**
     * Base y cuota redondeadas a céntimo. El total es su suma.
     * Opción B, a la espera de que el gestor la confirme: no se redondea una suma
     * sin redondear. Frente al total del presupuesto cabe una diferencia de 0,01 €.
     */
    private void calcularTotales(Factura factura) {
        boolean ivaHabilitado = Boolean.TRUE.equals(factura.getIvaHabilitado());
        List<CalculoFactura.LineaEntrada> entradas = new java.util.ArrayList<>();
        for (FacturaItem item : factura.getItems()) {
            entradas.add(new CalculoFactura.LineaEntrada(
                    BigDecimal.valueOf(item.getSubtotal() == null ? 0.0 : item.getSubtotal()),
                    TipoIva.resolver(item.getTipoIva(), item.getAplicaIva())
            ));
        }
        BigDecimal anticipo = factura.getTipoFactura() == TipoFactura.FINAL_CON_ANTICIPO
                ? factura.getImporteAnticipoDescontado()
                : null;
        CalculoFactura.Resultado totales = CalculoFactura.calcular(entradas, ivaHabilitado, anticipo);
        factura.setSubtotal(totales.subtotal().doubleValue());
        factura.setIva(totales.iva().doubleValue());
        factura.setTotal(totales.total().doubleValue());

        List<FacturaItem> items = factura.getItems();
        for (int i = 0; i < items.size(); i++) {
            FacturaItem item = items.get(i);
            int tipo = totales.tipos().get(i);
            item.setTipoIva(tipo);
            item.setAplicaIva(tipo != TipoIva.CERO);
            item.setCuotaIva(totales.cuotas().get(i).doubleValue());
        }
    }

    /** Recalcula subtotal, IVA y total (incluye factura final con anticipo descontado). Expuesto para {@link AnticipoService}. */
    public void recalcularTotales(Factura factura) {
        calcularTotales(factura);
    }

    /** Validación de datos fiscales mínimos para emitir factura (reutilizado por anticipos). */
    public void validarEmisionFactura(Long usuarioId, Cliente cliente) {
        validarDatosFactura(usuarioId, cliente);
    }
}
