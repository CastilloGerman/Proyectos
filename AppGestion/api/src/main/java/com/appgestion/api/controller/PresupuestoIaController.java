package com.appgestion.api.controller;

import com.appgestion.api.dto.request.PresupuestoIaRequest;
import com.appgestion.api.dto.response.PresupuestoIaBorradorResponse;
import com.appgestion.api.service.CurrentUserService;
import com.appgestion.api.service.PresupuestoIaService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/presupuestos/ia")
public class PresupuestoIaController {

    private final PresupuestoIaService presupuestoIaService;
    private final CurrentUserService currentUserService;

    public PresupuestoIaController(PresupuestoIaService presupuestoIaService, CurrentUserService currentUserService) {
        this.presupuestoIaService = presupuestoIaService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/borrador")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public PresupuestoIaBorradorResponse generarBorrador(@Valid @RequestBody PresupuestoIaRequest request) {
        return presupuestoIaService.generarBorrador(request, currentUserService.getCurrentUsuario());
    }
}
