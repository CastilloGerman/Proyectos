package com.appgestion.api.controller;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.FiscalCriterioImputacion;
import com.appgestion.api.dto.response.FiscalPlazoActualResponse;
import com.appgestion.api.dto.response.Modelo303ResumenResponse;
import com.appgestion.api.dto.response.Modelo347ResumenResponse;
import com.appgestion.api.service.CurrentUserService;
import com.appgestion.api.service.FiscalPdfService;
import com.appgestion.api.service.FiscalPlazosService;
import com.appgestion.api.service.FiscalService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/fiscal")
public class FiscalController {

    private final FiscalService fiscalService;
    private final FiscalPdfService fiscalPdfService;
    private final FiscalPlazosService fiscalPlazosService;
    private final CurrentUserService currentUserService;

    public FiscalController(
            FiscalService fiscalService,
            FiscalPdfService fiscalPdfService,
            FiscalPlazosService fiscalPlazosService,
            CurrentUserService currentUserService
    ) {
        this.fiscalService = fiscalService;
        this.fiscalPdfService = fiscalPdfService;
        this.fiscalPlazosService = fiscalPlazosService;
        this.currentUserService = currentUserService;
    }

    /**
     * Próximo plazo orientativo de presentación del Modelo 303 (sin persistencia; cálculo en tiempo real).
     */
    @GetMapping("/plazo-actual")
    public FiscalPlazoActualResponse plazoActual() {
        currentUserService.getCurrentUsuario();
        return fiscalPlazosService.calcularPlazoActual();
    }

    /**
     * Resumen orientativo Modelo 303 (ventas / IVA repercutido) por trimestre.
     */
    @GetMapping("/modelo303")
    public Modelo303ResumenResponse modelo303(
            @RequestParam("year") int year,
            @RequestParam("trimestre") int trimestre,
            @RequestParam(value = "criterio", defaultValue = "DEVENGO") String criterio,
            @RequestParam(value = "soloPagadas", defaultValue = "false") boolean soloPagadas
    ) {
        Usuario u = currentUserService.getCurrentUsuario();
        FiscalCriterioImputacion c = parseCriterioSafe(criterio);
        try {
            return fiscalService.resumenModelo303(u.getId(), year, trimestre, c, soloPagadas);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping(value = "/modelo303/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> modelo303Pdf(
            @RequestParam("year") int year,
            @RequestParam("trimestre") int trimestre,
            @RequestParam(value = "criterio", defaultValue = "DEVENGO") String criterio,
            @RequestParam(value = "soloPagadas", defaultValue = "false") boolean soloPagadas
    ) {
        Usuario u = currentUserService.getCurrentUsuario();
        FiscalCriterioImputacion c = parseCriterioSafe(criterio);
        Modelo303ResumenResponse resumen;
        try {
            resumen = fiscalService.resumenModelo303(u.getId(), year, trimestre, c, soloPagadas);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        byte[] pdf = fiscalPdfService.generarPdfModelo303(resumen);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment",
                "modelo303-resumen-" + year + "-T" + trimestre + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /**
     * Clientes con base anual (devengo) por encima del umbral orientativo 347.
     */
    @GetMapping("/modelo347")
    public Modelo347ResumenResponse modelo347(@RequestParam("year") int year) {
        Usuario u = currentUserService.getCurrentUsuario();
        try {
            return fiscalService.resumenModelo347(u.getId(), year);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private static FiscalCriterioImputacion parseCriterioSafe(String criterio) {
        try {
            return FiscalService.parseCriterio(criterio);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
