package com.appgestion.api.controller;

import com.appgestion.api.service.CurrentUserService;
import com.appgestion.api.service.SupportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth/support")
public class SupportController {

    private final CurrentUserService currentUserService;
    private final SupportService supportService;

    public SupportController(CurrentUserService currentUserService, SupportService supportService) {
        this.currentUserService = currentUserService;
        this.supportService = supportService;
    }

    @PostMapping(value = "/contact", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, String>> contact(
            @RequestParam("asunto") String asunto,
            @RequestParam("mensaje") String mensaje,
            @RequestParam(value = "archivos", required = false) List<MultipartFile> archivos
    ) throws IOException {
        var usuario = currentUserService.getCurrentUsuario();
        supportService.enviarContactoSoporte(usuario, asunto, mensaje, archivos);
        return ResponseEntity.ok(Map.of(
                "message",
                "Tu mensaje se ha enviado al equipo de soporte. Te responderemos lo antes posible."
        ));
    }
}
