package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Factura;
import com.appgestion.api.dto.request.EnviarEmailRequest;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.repository.FacturaRepository;
import com.appgestion.api.util.EmailCopy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FacturaEmailService {

    private final FacturaRepository facturaRepository;
    private final EmpresaRepository empresaRepository;
    private final FacturaPdfService facturaPdfService;
    private final EmailService emailService;

    public FacturaEmailService(FacturaRepository facturaRepository,
                               EmpresaRepository empresaRepository,
                               FacturaPdfService facturaPdfService,
                               EmailService emailService) {
        this.facturaRepository = facturaRepository;
        this.empresaRepository = empresaRepository;
        this.facturaPdfService = facturaPdfService;
        this.emailService = emailService;
    }

    /** No usar readOnly: encola fila en {@code email_jobs} (escritura). */
    @Transactional
    public void enviarPorEmail(Long id, Long usuarioId, EnviarEmailRequest request) {
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
        String nombreEmpresa = empresaRepository.findByUsuarioId(usuarioId).map(e -> e.getNombre()).orElse(null);
        String nombreCliente = factura.getCliente() != null ? factura.getCliente().getNombre() : null;
        String cuerpo = EmailCopy.prefijoClienteEmpresa(nombreCliente, nombreEmpresa)
                + "<p>Adjunto encontrará la factura correspondiente.</p><p>Saludos cordiales.</p>";
        emailService.enviarPdf(usuarioId, email, asunto, cuerpo, pdf, nombreArchivo);
    }
}
