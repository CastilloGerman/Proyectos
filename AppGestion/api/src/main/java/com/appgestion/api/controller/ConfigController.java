package com.appgestion.api.controller;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.dto.request.EmpresaRequest;
import com.appgestion.api.dto.request.DatosFiscalesPatchRequest;
import com.appgestion.api.dto.request.MetodosCobroPatchRequest;
import com.appgestion.api.dto.request.PlantillasPdfPatchRequest;
import com.appgestion.api.dto.request.PlantillasPdfPreviewRequest;
import com.appgestion.api.dto.response.EmpresaResponse;
import com.appgestion.api.service.CurrentUserService;
import com.appgestion.api.service.EmpresaService;
import com.appgestion.api.service.PlantillasPdfPreviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/config")
public class ConfigController {

    private final EmpresaService empresaService;
    private final CurrentUserService currentUserService;
    private final PlantillasPdfPreviewService plantillasPdfPreviewService;

    public ConfigController(
            EmpresaService empresaService,
            CurrentUserService currentUserService,
            PlantillasPdfPreviewService plantillasPdfPreviewService) {
        this.empresaService = empresaService;
        this.currentUserService = currentUserService;
        this.plantillasPdfPreviewService = plantillasPdfPreviewService;
    }

    @GetMapping("/empresa")
    public EmpresaResponse obtenerEmpresa() {
        Usuario usuario = currentUserService.getCurrentUsuario();
        return empresaService.obtenerPorUsuario(usuario.getId());
    }

    @PutMapping("/empresa")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public EmpresaResponse guardarEmpresa(@Valid @RequestBody EmpresaRequest request) {
        Usuario usuario = currentUserService.getCurrentUsuario();
        return empresaService.guardar(request, usuario);
    }

    /** Valores por defecto de cobro en facturas (IBAN, Bizum, método y condiciones). */
    @PatchMapping("/empresa/metodos-cobro")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public EmpresaResponse actualizarMetodosCobro(@Valid @RequestBody MetodosCobroPatchRequest request) {
        Usuario usuario = currentUserService.getCurrentUsuario();
        return empresaService.actualizarMetodosCobro(request, usuario);
    }

    @PatchMapping("/empresa/datos-fiscales")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public EmpresaResponse actualizarDatosFiscales(@Valid @RequestBody DatosFiscalesPatchRequest request) {
        Usuario usuario = currentUserService.getCurrentUsuario();
        return empresaService.actualizarDatosFiscales(request, usuario);
    }

    /** Textos al pie de PDFs de presupuesto y factura (sin tocar logo, firma ni correo). */
    @PatchMapping("/empresa/plantillas-pdf")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public EmpresaResponse actualizarPlantillasPdf(@Valid @RequestBody PlantillasPdfPatchRequest request) {
        Usuario usuario = currentUserService.getCurrentUsuario();
        return empresaService.actualizarPlantillasPdf(request, usuario);
    }

    /**
     * PDF de muestra generado con OpenPDF (misma lógica que facturas/presupuestos reales).
     * {@code notasPie} null = usar textos guardados en empresa.
     */
    /**
     * Sin {@code consumes} estricto: evita 404 por negociación de Content-Type (p. ej. charset) en algunos clientes.
     */
    @PostMapping(value = "/empresa/plantillas-pdf/preview", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<byte[]> plantillasPdfPreview(@Valid @RequestBody PlantillasPdfPreviewRequest request) {
        Usuario usuario = currentUserService.getCurrentUsuario();
        byte[] pdf = plantillasPdfPreviewService.generar(usuario.getId(), request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"vista-previa-plantilla.pdf\"")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_PDF))
                .body(pdf);
    }
}
