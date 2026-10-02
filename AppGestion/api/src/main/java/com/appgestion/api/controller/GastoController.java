package com.appgestion.api.controller;

import com.appgestion.api.dto.request.GastoRequest;
import com.appgestion.api.dto.request.GastoPresupuestoPatchRequest;
import com.appgestion.api.dto.response.GastoResponse;
import com.appgestion.api.dto.response.GastoBorradorResponse;
import com.appgestion.api.service.CurrentUserService;
import com.appgestion.api.service.GastoService;
import com.appgestion.api.service.GastoIaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/gastos")
public class GastoController {

    private final GastoService gastoService;
    private final GastoIaService gastoIaService;
    private final CurrentUserService currentUserService;

    public GastoController(
            GastoService gastoService,
            GastoIaService gastoIaService,
            CurrentUserService currentUserService
    ) {
        this.gastoService = gastoService;
        this.gastoIaService = gastoIaService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<GastoResponse> listar(@RequestParam(required = false) Long presupuestoId) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        return gastoService.listar(usuarioId, presupuestoId);
    }

    @GetMapping("/{id:\\d+}")
    public GastoResponse obtenerPorId(@PathVariable Long id) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        return gastoService.obtenerPorId(id, usuarioId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GastoResponse crear(@Valid @RequestBody GastoRequest request) {
        var usuario = currentUserService.getCurrentUsuario();
        return gastoService.crear(request, usuario);
    }

    @PostMapping(value = "/ia/extraer", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GastoBorradorResponse extraerBorrador(@RequestParam("archivo") MultipartFile archivo) {
        return gastoIaService.extraerBorrador(archivo);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public GastoResponse actualizar(@PathVariable Long id, @Valid @RequestBody GastoRequest request) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        return gastoService.actualizar(id, request, usuarioId);
    }

    @PatchMapping("/{id}/presupuesto")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public GastoResponse asignarPresupuesto(
            @PathVariable Long id,
            @RequestBody GastoPresupuestoPatchRequest request) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        return gastoService.asignarPresupuesto(id, request, usuarioId);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        Long usuarioId = currentUserService.getCurrentUsuario().getId();
        gastoService.eliminar(id, usuarioId);
    }
}
