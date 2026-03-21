package com.appgestion.api.controller;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.dto.request.EmpresaRequest;
import com.appgestion.api.dto.request.DatosFiscalesPatchRequest;
import com.appgestion.api.dto.request.MetodosCobroPatchRequest;
import com.appgestion.api.dto.request.PlantillasPdfPatchRequest;
import com.appgestion.api.dto.response.EmpresaResponse;
import com.appgestion.api.service.CurrentUserService;
import com.appgestion.api.service.EmpresaService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/config")
public class ConfigController {

    private final EmpresaService empresaService;
    private final CurrentUserService currentUserService;

    public ConfigController(EmpresaService empresaService, CurrentUserService currentUserService) {
        this.empresaService = empresaService;
        this.currentUserService = currentUserService;
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
}
