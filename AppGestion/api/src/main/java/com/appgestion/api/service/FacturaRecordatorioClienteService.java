package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Empresa;
import com.appgestion.api.domain.entity.Factura;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.repository.FacturaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Recordatorios por email al <strong>cliente</strong> (no al autónomo), a los 7, 15 o 30 días
 * <strong>después</strong> de la fecha de vencimiento, si la empresa lo tiene activo y correo SMTP configurado.
 */
@Service
public class FacturaRecordatorioClienteService {

    private static final Logger log = LoggerFactory.getLogger(FacturaRecordatorioClienteService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Set<Integer> DIAS_PERMITIDOS = Set.of(7, 15, 30);

    private final FacturaRepository facturaRepository;
    private final EmpresaRepository empresaRepository;
    private final EmailService emailService;
    private final FacturaPdfService facturaPdfService;

    public FacturaRecordatorioClienteService(
            FacturaRepository facturaRepository,
            EmpresaRepository empresaRepository,
            EmailService emailService,
            FacturaPdfService facturaPdfService) {
        this.facturaRepository = facturaRepository;
        this.empresaRepository = empresaRepository;
        this.emailService = emailService;
        this.facturaPdfService = facturaPdfService;
    }

    @Transactional
    public void enviarRecordatorios() {
        LocalDate hoy = LocalDate.now();
        List<LocalDate> fechas = List.of(hoy.minusDays(7), hoy.minusDays(15), hoy.minusDays(30));
        List<Factura> facturas = facturaRepository.findImpagadasConFechaVencimientoEn(fechas);
        if (facturas.isEmpty()) {
            return;
        }

        log.info("Recordatorios al cliente: candidatas {} facturas", facturas.size());

        for (Factura f : facturas) {
            int diasRetraso = (int) ChronoUnit.DAYS.between(f.getFechaVencimiento(), hoy);
            if (!DIAS_PERMITIDOS.contains(diasRetraso)) {
                continue;
            }
            try {
                procesarFactura(f, diasRetraso);
            } catch (Exception e) {
                log.warn("No se pudo enviar recordatorio al cliente (factura {}): {}", f.getId(), e.getMessage());
            }
        }
    }

    private void procesarFactura(Factura factura, int diasRetraso) throws Exception {
        double pendiente = importePendiente(factura);
        if (pendiente <= 0.009) {
            return;
        }

        Empresa emp = empresaRepository.findByUsuarioId(factura.getUsuario().getId()).orElse(null);
        if (emp == null || !Boolean.TRUE.equals(emp.getRecordatorioClienteActivo())) {
            return;
        }
        String emailCliente = factura.getCliente() != null ? factura.getCliente().getEmail() : null;
        if (!StringUtils.hasText(emailCliente)) {
            return;
        }
        final String emailDestino = Objects.requireNonNull(emailCliente).trim();

        List<Integer> habilitados = parseDiasEmpresa(emp.getRecordatorioClienteDias());
        if (!habilitados.contains(diasRetraso)) {
            return;
        }
        if (yaEnviado(factura, diasRetraso)) {
            return;
        }

        enviarCorreo(factura, emp, pendiente, emailDestino);
        marcarEnviado(factura, diasRetraso);
        facturaRepository.save(factura);
    }

    /**
     * Envío manual desde la lista de facturas: mismo correo que el automático (sin activar marcas de job).
     * Solo si el vencimiento está a ≤15 días (incluye ya vencidas).
     */
    @Transactional
    public void enviarRecordatorioClienteManual(Long usuarioId, Long facturaId) throws Exception {
        Factura f = facturaRepository.findByIdAndUsuarioIdWithRelaciones(facturaId, usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada."));
        double pendiente = importePendiente(f);
        if (pendiente <= 0.009) {
            throw new IllegalStateException("No hay importe pendiente en esta factura.");
        }
        LocalDate venc = f.getFechaVencimiento();
        if (venc == null) {
            throw new IllegalStateException("La factura no tiene fecha de vencimiento.");
        }
        LocalDate hoy = LocalDate.now();
        long diasHastaVenc = ChronoUnit.DAYS.between(hoy, venc);
        if (diasHastaVenc > 15) {
            throw new IllegalStateException(
                    "El recordatorio manual solo está disponible cuando faltan 15 días o menos para el vencimiento (o ya ha vencido).");
        }
        Empresa emp = empresaRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new IllegalStateException("No hay datos de empresa."));
        String emailCliente = f.getCliente() != null ? f.getCliente().getEmail() : null;
        if (!StringUtils.hasText(emailCliente)) {
            throw new IllegalStateException("El cliente no tiene email. Añádelo en la ficha del cliente.");
        }
        enviarCorreo(f, emp, pendiente, Objects.requireNonNull(emailCliente).trim());
    }

    private static double importePendiente(Factura f) {
        String estado = f.getEstadoPago() != null ? f.getEstadoPago() : "";
        if ("Pagada".equalsIgnoreCase(estado)) {
            return 0;
        }
        double total = Optional.ofNullable(f.getTotal()).orElse(0.0);
        if ("Parcial".equalsIgnoreCase(estado)) {
            double cobrado = Optional.ofNullable(f.getMontoCobrado()).orElse(0.0);
            return Math.max(0, total - cobrado);
        }
        return total;
    }

    private static List<Integer> parseDiasEmpresa(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of(7, 15, 30);
        }
        List<Integer> out = new ArrayList<>();
        for (String part : raw.split(",")) {
            String t = part.trim();
            if (t.isEmpty()) {
                continue;
            }
            try {
                int d = Integer.parseInt(t);
                if (DIAS_PERMITIDOS.contains(d)) {
                    out.add(d);
                }
            } catch (NumberFormatException ignored) {
                // omitir
            }
        }
        out.sort(Comparator.naturalOrder());
        return out.isEmpty() ? List.of(7, 15, 30) : out;
    }

    private static boolean yaEnviado(Factura f, int diasRetraso) {
        String m = f.getRecordatorioClienteMarcas();
        if (m == null || m.isBlank()) {
            return false;
        }
        String token = String.valueOf(diasRetraso);
        for (String p : m.split(",")) {
            if (token.equals(p.trim())) {
                return true;
            }
        }
        return false;
    }

    private static void marcarEnviado(Factura f, int diasRetraso) {
        Set<String> set = new LinkedHashSet<>();
        String m = f.getRecordatorioClienteMarcas();
        if (m != null && !m.isBlank()) {
            for (String p : m.split(",")) {
                String t = p.trim();
                if (!t.isEmpty()) {
                    set.add(t);
                }
            }
        }
        set.add(String.valueOf(diasRetraso));
        List<String> sorted = new ArrayList<>(set);
        sorted.sort(Comparator.comparingInt(Integer::parseInt));
        f.setRecordatorioClienteMarcas(String.join(",", sorted));
    }

    private void enviarCorreo(Factura f, Empresa emp, double pendiente, String emailDestino) throws Exception {
        Usuario u = f.getUsuario();
        String nombreEmisor = emp.getNombre() != null && !emp.getNombre().isBlank()
                ? emp.getNombre()
                : (u.getNombre() != null ? u.getNombre() : "—");
        String clienteNombre = f.getCliente() != null ? f.getCliente().getNombre() : "—";
        String numero = f.getNumeroFactura();
        String importe = String.format(Locale.ROOT, "%.2f €", pendiente);
        LocalDate venc = f.getFechaVencimiento();
        String fechaVenc = venc != null ? venc.format(DATE_FMT) : "—";
        LocalDate hoy = LocalDate.now();

        String asunto = String.format("Recordatorio de pago: factura %s · %s", numero, nombreEmisor);

        StringBuilder cuerpo = new StringBuilder();
        cuerpo.append("<p>Estimado/a ").append(escapeHtml(clienteNombre)).append(",</p>");
        cuerpo.append("<p>Le recordamos la factura <strong>").append(escapeHtml(numero)).append("</strong>");
        cuerpo.append(" con fecha de vencimiento el <strong>").append(fechaVenc).append("</strong>");
        cuerpo.append(" e importe <strong>pendiente de ").append(importe).append("</strong>.</p>");
        if (venc != null) {
            if (venc.isBefore(hoy)) {
                long diasRetraso = ChronoUnit.DAYS.between(venc, hoy);
                cuerpo.append("<p>Han transcurrido <strong>").append(diasRetraso).append("</strong> días desde el vencimiento.</p>");
            } else if (venc.isAfter(hoy)) {
                long faltan = ChronoUnit.DAYS.between(hoy, venc);
                cuerpo.append("<p>La factura <strong>vence dentro de ").append(faltan).append("</strong> día(s).</p>");
            } else {
                cuerpo.append("<p>La factura <strong>vence hoy</strong>.</p>");
            }
        }
        if (StringUtils.hasText(f.getPaymentLinkUrl())) {
            cuerpo.append("<p>Puede utilizar el siguiente enlace para realizar el pago: ")
                    .append("<a href=\"").append(f.getPaymentLinkUrl()).append("\">Pagar</a></p>");
        }
        cuerpo.append("<p>Si ya ha realizado el pago, puede ignorar este mensaje. ")
                .append("Para cualquier aclaración, responda a este correo.</p>");
        cuerpo.append("<p>Adjuntamos la factura en formato PDF.</p>");
        cuerpo.append("<p style='color:#666;font-size:12px'>Mensaje enviado automáticamente por ")
                .append(escapeHtml(nombreEmisor)).append(".</p>");

        Long uid = u.getId();
        byte[] pdf = facturaPdfService.generarPdf(f, uid);
        String nombrePdf = "factura-"
                + (numero != null && !numero.isBlank() ? numero.replaceAll("[^a-zA-Z0-9._-]", "_") : "documento")
                + ".pdf";

        String replyTo = StringUtils.hasText(u.getEmail()) ? u.getEmail() : emp.getEmail();
        emailService.enviarHtmlACliente(uid, emailDestino, replyTo, asunto, cuerpo.toString(), pdf, nombrePdf);
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
