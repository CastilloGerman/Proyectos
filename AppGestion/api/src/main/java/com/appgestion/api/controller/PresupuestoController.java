package com.appgestion.api.controller;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.dto.request.EnviarEmailRequest;
import com.appgestion.api.dto.request.PresupuestoCondicionesPredeterminadasRequest;
import com.appgestion.api.dto.request.AnticipoRegistroRequest;
import com.appgestion.api.dto.request.PresupuestoRequest;
import com.appgestion.api.dto.response.AnticipoResumenDTO;
import com.appgestion.api.dto.response.FacturaResponse;
import com.appgestion.api.dto.response.PresupuestoCondicionDisponibleResponse;
import com.appgestion.api.dto.response.PresupuestoResponse;
import com.appgestion.api.service.AnticipoService;
import com.appgestion.api.service.CurrentUserService;
import com.appgestion.api.service.FacturaService;
import com.appgestion.api.service.PresupuestoCondicionesService;
import com.appgestion.api.service.PresupuestoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;
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
    private final AnticipoService anticipoService;
    private final CurrentUserService currentUserService;
    private final PresupuestoCondicionesService presupuestoCondicionesService;

    public PresupuestoController(PresupuestoService presupuestoService,
                                 FacturaService facturaService,
                                 AnticipoService anticipoService,
                                 CurrentUserService currentUserService,
                                 PresupuestoCondicionesService presupuestoCondicionesService) {
        this.presupuestoService = presupuestoService;
        this.facturaService = facturaService;
        this.anticipoService = anticipoService;
        this.currentUserService = currentUserService;
        this.presupuestoCondicionesService = presupuestoCondicionesService;
    }

    @GetMapping("/condiciones-disponibles")
    public List<PresupuestoCondicionDisponibleResponse> condicionesDisponibles() {
        currentUserService.getCurrentUsuario();
        return presupuestoCondicionesService.listarDisponibles();
    }

    @GetMapping("/mis-condiciones-predeterminadas")
    public List<String> misCondicionesPredeterminadas() {
        Long uid = currentUserService.getCurrentUsuario().getId();
        return presupuestoService.obtenerMisCondicionesPredeterminadas(uid);
    }

    @PutMapping("/mis-condiciones-predeterminadas")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public void guardarMisCondicionesPredeterminadas(@Valid @RequestBody(required = false) PresupuestoCondicionesPredeterminadasRequest body) {
        Long uid = currentUserService.getCurrentUsuario().getId();
        List<String> claves = body != null && body.claves() != null ? body.claves() : List.of();
        presupuestoService.guardarMisCondicionesPredeterminadas(uid, claves);
    }

    @GetMapping
    public List<PresupuestoResponse> listar(@RequestParam(required = false) String q) {
        Usuario usuario = currentUserService.getCurrentUsuario();
        return presupuestoService.listar(usuario.getId(), q);
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
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enviarPorEmail(@PathVariable Long id, @Valid @RequestBody(required = false) EnviarEmailRequest request) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        try {
            presupuestoService.enviarPorEmail(id, usuarioId, request);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    e.getMessage());
        } catch (RuntimeException e) {
            log.warn("Error al enviar email presupuesto {}: {}", id, e.getMessage(), e);
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al encolar el envío. Inténtalo más tarde.");
        }
    }

    @PostMapping("/{id}/factura")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @ResponseStatus(HttpStatus.CREATED)
    public FacturaResponse crearFacturaDesdePresupuesto(@PathVariable Long id) {
        Usuario usuario = currentUserService.getCurrentUsuario();
        return facturaService.crearDesdePresupuesto(id, usuario);
    }

    @PostMapping("/{id}/anticipo")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public PresupuestoResponse registrarAnticipo(@PathVariable Long id, @Valid @RequestBody AnticipoRegistroRequest body) {
        Long uid = currentUserService.getCurrentUsuario().getId();
        anticipoService.registrarAnticipo(id, body.importeAnticipo(), body.fechaAnticipo(), uid);
        return presupuestoService.obtenerPorId(id, uid);
    }

    @PostMapping("/{id}/factura-anticipo")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @ResponseStatus(HttpStatus.CREATED)
    public FacturaResponse generarFacturaAnticipo(@PathVariable Long id) {
        Usuario usuario = currentUserService.getCurrentUsuario();
        return anticipoService.generarFacturaAnticipo(id, usuario.getId(), usuario);
    }

    @PostMapping("/{id}/factura-final")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @ResponseStatus(HttpStatus.CREATED)
    public FacturaResponse generarFacturaFinal(@PathVariable Long id) {
        Usuario usuario = currentUserService.getCurrentUsuario();
        return anticipoService.generarFacturaFinal(id, usuario.getId(), usuario);
    }

    @GetMapping("/{id}/resumen-anticipo")
    public AnticipoResumenDTO resumenAnticipo(@PathVariable Long id) {
        Long uid = currentUserService.getCurrentUsuario().getId();
        return anticipoService.calcularResumenAnticipo(id, uid);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PresupuestoResponse crear(@Valid @RequestBody PresupuestoRequest request) {
        Usuario usuario = currentUserService.getCurrentUsuario();
        return presupuestoService.crear(request, usuario);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public PresupuestoResponse actualizar(@PathVariable Long id, @Valid @RequestBody PresupuestoRequest request) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        return presupuestoService.actualizar(id, request, usuarioId);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        presupuestoService.eliminar(id, usuarioId);
    }
}
