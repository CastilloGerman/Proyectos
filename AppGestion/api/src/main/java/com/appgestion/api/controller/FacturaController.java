package com.appgestion.api.controller;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.dto.request.EnviarEmailRequest;
import com.appgestion.api.dto.request.FacturaCobroRequest;
import com.appgestion.api.dto.request.FacturaRequest;
import com.appgestion.api.dto.response.FacturaResponse;
import com.appgestion.api.service.FacturaRecordatorioClienteService;
import com.appgestion.api.service.FacturaService;
import com.appgestion.api.service.CurrentUserService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/facturas")
public class FacturaController {

    private final FacturaService facturaService;
    private final CurrentUserService currentUserService;
    private final FacturaRecordatorioClienteService facturaRecordatorioClienteService;

    public FacturaController(
            FacturaService facturaService,
            CurrentUserService currentUserService,
            FacturaRecordatorioClienteService facturaRecordatorioClienteService) {
        this.facturaService = facturaService;
        this.currentUserService = currentUserService;
        this.facturaRecordatorioClienteService = facturaRecordatorioClienteService;
    }

    @GetMapping
    public List<FacturaResponse> listar(@RequestParam(required = false) String q) {
        Usuario usuario = currentUserService.getCurrentUsuario();
        return facturaService.listar(usuario.getId(), q);
    }

    @GetMapping("/{id}")
    public FacturaResponse obtenerPorId(@PathVariable Long id) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        return facturaService.obtenerPorId(id, usuarioId);
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        byte[] pdf = facturaService.generarPdf(id, usuarioId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", "factura-" + id + ".pdf");
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/pdf"))
                .body(pdf);
    }

    /**
     * Recordatorio de cobro al cliente (mismo contenido base que el envío automático). No modifica marcas del job.
     * Requiere vencimiento en ≤15 días o ya vencido, importe pendiente y email del cliente.
     * Ruta en segmentos para evitar que Spring trate la petición como recurso estático.
     */
    @PostMapping("/{id}/recordatorio/cobro")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enviarRecordatorioCliente(@PathVariable Long id) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        try {
            facturaRecordatorioClienteService.enviarRecordatorioClienteManual(usuarioId, id);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    e.getMessage());
        } catch (MessagingException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "No se pudo enviar el correo. Revisa el SMTP en datos de la empresa: "
                            + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(FacturaController.class).warn(
                    "Error recordatorio cliente factura {}: {}", id, e.getMessage(), e);
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al enviar el recordatorio: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    @PostMapping("/{id}/enviar-email")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enviarPorEmail(@PathVariable Long id, @RequestBody(required = false) EnviarEmailRequest request) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        try {
            facturaService.enviarPorEmail(id, usuarioId, request);
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    e.getMessage());
        } catch (jakarta.mail.MessagingException | RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger(FacturaController.class).warn("Error al enviar email factura {}: {}", id, e.getMessage(), e);
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al enviar el email: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FacturaResponse crear(@Valid @RequestBody FacturaRequest request) {
        Usuario usuario = currentUserService.getCurrentUsuario();
        return facturaService.crear(request, usuario);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public FacturaResponse actualizar(@PathVariable Long id, @Valid @RequestBody FacturaRequest request) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        return facturaService.actualizar(id, request, usuarioId);
    }

    @PostMapping("/{id}/cobros")
    public FacturaResponse registrarCobro(@PathVariable Long id, @Valid @RequestBody FacturaCobroRequest request) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        return facturaService.registrarCobro(id, usuarioId, request);
    }

    @PostMapping("/{id}/payment-link")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public FacturaResponse generarPaymentLink(@PathVariable Long id) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        return facturaService.generarPaymentLink(id, usuarioId);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        facturaService.eliminar(id, usuarioId);
    }
}
