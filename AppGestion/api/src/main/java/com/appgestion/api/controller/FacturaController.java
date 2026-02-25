package com.appgestion.api.controller;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.dto.request.EnviarEmailRequest;
import com.appgestion.api.dto.request.FacturaRequest;
import com.appgestion.api.dto.response.FacturaResponse;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.security.SecurityUtils;
import com.appgestion.api.service.FacturaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/facturas")
public class FacturaController {

    private final FacturaService facturaService;
    private final UsuarioRepository usuarioRepository;

    public FacturaController(FacturaService facturaService, UsuarioRepository usuarioRepository) {
        this.facturaService = facturaService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public List<FacturaResponse> listar() {
        Usuario usuario = SecurityUtils.getCurrentUsuario(usuarioRepository);
        return facturaService.listar(usuario.getId());
    }

    @GetMapping("/{id}")
    public FacturaResponse obtenerPorId(@PathVariable Long id) {
        Long usuarioId = SecurityUtils.getCurrentUsuario(usuarioRepository).getId();
        return facturaService.obtenerPorId(id, usuarioId);
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) {
        Long usuarioId = SecurityUtils.getCurrentUsuario(usuarioRepository).getId();
        byte[] pdf = facturaService.generarPdf(id, usuarioId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", "factura-" + id + ".pdf");
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/pdf"))
                .body(pdf);
    }

    @PostMapping("/{id}/enviar-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enviarPorEmail(@PathVariable Long id, @RequestBody(required = false) EnviarEmailRequest request) {
        Long usuarioId = SecurityUtils.getCurrentUsuario(usuarioRepository).getId();
        try {
            facturaService.enviarPorEmail(id, usuarioId, request);
        } catch (jakarta.mail.MessagingException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al enviar el email: " + e.getMessage());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FacturaResponse crear(@Valid @RequestBody FacturaRequest request) {
        Usuario usuario = SecurityUtils.getCurrentUsuario(usuarioRepository);
        return facturaService.crear(request, usuario);
    }

    @PutMapping("/{id}")
    public FacturaResponse actualizar(@PathVariable Long id, @Valid @RequestBody FacturaRequest request) {
        Long usuarioId = SecurityUtils.getCurrentUsuario(usuarioRepository).getId();
        return facturaService.actualizar(id, request, usuarioId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        Long usuarioId = SecurityUtils.getCurrentUsuario(usuarioRepository).getId();
        facturaService.eliminar(id, usuarioId);
    }
}
