package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Factura;
import com.appgestion.api.repository.FacturaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class FacturaRecordatorioService {

    private static final Logger log = LoggerFactory.getLogger(FacturaRecordatorioService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int DIAS_AVISO_PREVIO = 3;

    private final FacturaRepository facturaRepository;
    private final EmailService emailService;

    public FacturaRecordatorioService(FacturaRepository facturaRepository, EmailService emailService) {
        this.facturaRepository = facturaRepository;
        this.emailService = emailService;
    }

    @Transactional
    public void enviarRecordatorios() {
        LocalDate hasta = LocalDate.now().plusDays(DIAS_AVISO_PREVIO);
        List<Factura> facturas = facturaRepository.findFacturasParaRecordatorio(hasta);
        if (facturas.isEmpty()) return;

        log.info("Enviando recordatorios de cobro para {} facturas", facturas.size());

        for (Factura factura : facturas) {
            try {
                enviarRecordatorio(factura);
                factura.setRecordatorioEnviado(true);
                facturaRepository.save(factura);
            } catch (Exception e) {
                log.warn("No se pudo enviar recordatorio factura {}: {}", factura.getId(), e.getMessage());
            }
        }
    }

    private void enviarRecordatorio(Factura factura) throws Exception {
        String emailUsuario = factura.getUsuario().getEmail();
        if (emailUsuario == null || emailUsuario.isBlank()) return;

        LocalDate hoy = LocalDate.now();
        LocalDate venc = factura.getFechaVencimiento();
        boolean vencida = venc != null && venc.isBefore(hoy);

        String clienteNombre = factura.getCliente() != null ? factura.getCliente().getNombre() : "—";
        String numero = factura.getNumeroFactura();
        String importe = String.format("%.2f €", factura.getTotal());
        String fechaVenc = venc != null ? venc.format(DATE_FMT) : "—";

        String asunto = vencida
                ? "⚠ Factura vencida sin cobrar: " + numero + " · " + clienteNombre
                : "Recordatorio: factura próxima a vencer – " + numero + " · " + clienteNombre;

        String estado = vencida
                ? "<span style='color:#c62828'>VENCIDA el " + fechaVenc + "</span>"
                : "<span style='color:#e65100'>Vence el " + fechaVenc + "</span>";

        String cuerpo = "<p>Hola,</p>" +
                "<p>Te recordamos que tienes una factura pendiente de cobro:</p>" +
                "<table style='border-collapse:collapse;margin:16px 0'>" +
                "<tr><td style='padding:6px 16px 6px 0;color:#666'>Nº Factura</td><td><strong>" + numero + "</strong></td></tr>" +
                "<tr><td style='padding:6px 16px 6px 0;color:#666'>Cliente</td><td><strong>" + clienteNombre + "</strong></td></tr>" +
                "<tr><td style='padding:6px 16px 6px 0;color:#666'>Importe</td><td><strong>" + importe + "</strong></td></tr>" +
                "<tr><td style='padding:6px 16px 6px 0;color:#666'>Vencimiento</td><td>" + estado + "</td></tr>" +
                "</table>" +
                "<p>Accede a AppGestion para actualizar el estado de cobro.</p>" +
                "<p style='color:#999;font-size:12px'>Este mensaje ha sido generado automáticamente.</p>";

        emailService.enviarPdf(factura.getUsuario().getId(), emailUsuario, asunto, cuerpo, null, null);
    }
}
