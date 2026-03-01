package com.appgestion.api.controller;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.dto.request.EnviarEmailRequest;
import com.appgestion.api.dto.request.PresupuestoRequest;
import com.appgestion.api.dto.response.FacturaResponse;
import com.appgestion.api.dto.response.PresupuestoResponse;
import com.appgestion.api.service.CurrentUserService;
import com.appgestion.api.service.FacturaService;
import com.appgestion.api.service.PresupuestoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/presupuestos")
public class PresupuestoController {

    private static final Logger log = LoggerFactory.getLogger(PresupuestoController.class);

    private final PresupuestoService presupuestoService;
    private final FacturaService facturaService;
    private final CurrentUserService currentUserService;

    public PresupuestoController(PresupuestoService presupuestoService,
                                 FacturaService facturaService,
                                 CurrentUserService currentUserService) {
        this.presupuestoService = presupuestoService;
        this.facturaService = facturaService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<PresupuestoResponse> listar() {
        Usuario usuario = currentUserService.getCurrentUsuario();
        return presupuestoService.listar(usuario.getId());
    }

    @GetMapping("/{id}")
    public PresupuestoResponse obtenerPorId(@PathVariable Long id) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        return presupuestoService.obtenerPorId(id, usuarioId);
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        byte[] pdf = presupuestoService.generarPdf(id, usuarioId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", "presupuesto-" + id + ".pdf");
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/pdf"))
                .body(pdf);
    }

    @PostMapping("/{id}/enviar-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enviarPorEmail(@PathVariable Long id, @RequestBody(required = false) EnviarEmailRequest request) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        try {
            presupuestoService.enviarPorEmail(id, usuarioId, request);
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    e.getMessage());
        } catch (jakarta.mail.MessagingException | RuntimeException e) {
            log.warn("Error al enviar email presupuesto {}: {}", id, e.getMessage(), e);
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al enviar el email: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    @PostMapping("/{id}/factura")
    @ResponseStatus(HttpStatus.CREATED)
    public FacturaResponse crearFacturaDesdePresupuesto(@PathVariable Long id) {
        Usuario usuario = currentUserService.getCurrentUsuario();
        return facturaService.crearDesdePresupuesto(id, usuario);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PresupuestoResponse crear(@Valid @RequestBody PresupuestoRequest request) {
        Usuario usuario = currentUserService.getCurrentUsuario();
        return presupuestoService.crear(request, usuario);
    }

    @PutMapping("/{id}")
    public PresupuestoResponse actualizar(@PathVariable Long id, @Valid @RequestBody PresupuestoRequest request) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        return presupuestoService.actualizar(id, request, usuarioId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        presupuestoService.eliminar(id, usuarioId);
    }
}
