package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Presupuesto;
import com.appgestion.api.domain.entity.PresupuestoEnlace;
import com.appgestion.api.dto.response.PresupuestoEnlaceCreadoResponse;
import com.appgestion.api.dto.response.PresupuestoEnlaceEstadoResponse;
import com.appgestion.api.dto.response.PresupuestoPublicoResponse;
import com.appgestion.api.repository.PresupuestoEnlaceRepository;
import com.appgestion.api.repository.PresupuestoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
public class PublicBudgetLinkService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String UNAVAILABLE = "Este enlace no está disponible";
    private static final String ACTIVE_LINK_LIMIT = "Se alcanzó el máximo de enlaces activos para este presupuesto. Revoca o regenera los enlaces antes de volver a enviarlo.";
    private final PresupuestoRepository presupuestoRepository;
    private final PresupuestoEnlaceRepository enlaceRepository;
    private final PresupuestoPdfService pdfService;
    private final EmpresaService empresaService;
    private final PresupuestoCondicionesService condicionesService;
    private final NotificacionService notificacionService;
    private final int expiryDays;
    private final int maxActiveLinks;
    private final String frontendUrl;

    public PublicBudgetLinkService(PresupuestoRepository presupuestoRepository, PresupuestoEnlaceRepository enlaceRepository,
                                   PresupuestoPdfService pdfService, EmpresaService empresaService,
                                   PresupuestoCondicionesService condicionesService,
                                   NotificacionService notificacionService,
                                   @Value("${app.public-links.expiry-days:60}") int expiryDays,
                                   @Value("${app.public-links.max-active-links:10}") int maxActiveLinks,
                                   @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl) {
        this.presupuestoRepository = presupuestoRepository;
        this.enlaceRepository = enlaceRepository;
        this.pdfService = pdfService;
        this.empresaService = empresaService;
        this.condicionesService = condicionesService;
        this.notificacionService = notificacionService;
        if (expiryDays < 1) {
            throw new IllegalArgumentException("La caducidad de enlaces públicos debe ser de al menos un día");
        }
        if (maxActiveLinks < 1) {
            throw new IllegalArgumentException("El límite de enlaces activos debe ser al menos uno");
        }
        this.expiryDays = expiryDays;
        this.maxActiveLinks = maxActiveLinks;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public PresupuestoEnlaceCreadoResponse create(Long presupuestoId, Long usuarioId) {
        Presupuesto presupuesto = presupuestoRepository.findOwnedForUpdate(presupuestoId, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Presupuesto no encontrado"));
        return createLink(presupuesto, usuarioId);
    }

    @Transactional
    public PresupuestoEnlaceCreadoResponse regenerate(Long presupuestoId, Long usuarioId) {
        Presupuesto presupuesto = presupuestoRepository.findOwnedForUpdate(presupuestoId, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Presupuesto no encontrado"));
        enlaceRepository.revokeAll(presupuestoId, usuarioId);
        return createLink(presupuesto, usuarioId);
    }

    private PresupuestoEnlaceCreadoResponse createLink(Presupuesto presupuesto, Long usuarioId) {
        if (enlaceRepository.countByPresupuestoIdAndPresupuestoUsuarioIdAndRevocadoFalseAndExpiraAtAfter(
                presupuesto.getId(), usuarioId, Instant.now()) >= maxActiveLinks) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ACTIVE_LINK_LIMIT);
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = Instant.now();
        PresupuestoEnlace enlace = new PresupuestoEnlace();
        enlace.setPresupuesto(presupuesto);
        enlace.setTokenHash(hash(token));
        enlace.setCreadoAt(now);
        enlace.setExpiraAt(now.plus(expiryDays, ChronoUnit.DAYS));
        enlace.setRevocado(false);
        enlaceRepository.save(enlace);
        return new PresupuestoEnlaceCreadoResponse(frontendUrl.replaceAll("/+$", "") + "/p/" + token, enlace.getExpiraAt());
    }

    @Transactional
    public void revoke(Long presupuestoId, Long usuarioId) {
        if (!presupuestoRepository.existsByIdAndUsuarioId(presupuestoId, usuarioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Presupuesto no encontrado");
        }
        enlaceRepository.revokeAll(presupuestoId, usuarioId);
    }

    @Transactional(readOnly = true)
    public PresupuestoEnlaceEstadoResponse status(Long presupuestoId, Long usuarioId) {
        if (!presupuestoRepository.existsByIdAndUsuarioId(presupuestoId, usuarioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Presupuesto no encontrado");
        }
        Instant now = Instant.now();
        List<PresupuestoEnlace> links = enlaceRepository.findAllByPresupuestoIdAndPresupuestoUsuarioId(presupuestoId, usuarioId);
        List<PresupuestoEnlace> active = links.stream()
                .filter(link -> !link.isRevocado() && link.getExpiraAt().isAfter(now))
                .toList();
        Instant firstView = links.stream().map(PresupuestoEnlace::getPrimeraVistaAt)
                .filter(java.util.Objects::nonNull).min(Instant::compareTo).orElse(null);
        Instant lastView = links.stream().map(PresupuestoEnlace::getUltimaVistaAt)
                .filter(java.util.Objects::nonNull).max(Instant::compareTo).orElse(null);
        long views = links.stream().mapToLong(PresupuestoEnlace::getNumVistas).sum();
        Instant expires = active.stream().map(PresupuestoEnlace::getExpiraAt).max(Instant::compareTo).orElse(null);
        return new PresupuestoEnlaceEstadoResponse(!active.isEmpty(), expires, firstView, lastView, views, active.size());
    }

    @Transactional(readOnly = true)
    public PresupuestoPublicoResponse getPublic(String token) {
        PresupuestoEnlace enlace = resolve(token);
        Presupuesto presupuesto = enlace.getPresupuesto();
        var empresa = empresaService.getEmpresaOrNull(presupuesto.getUsuario().getId());
        List<PresupuestoPublicoResponse.PartidaPublica> items = presupuesto.getItems().stream()
                .filter(i -> !Boolean.FALSE.equals(i.getVisiblePdf()))
                .map(i -> new PresupuestoPublicoResponse.PartidaPublica(
                        i.getMaterial() != null ? i.getMaterial().getNombre() : i.getTareaManual(),
                        i.getCantidad(), i.getMaterial() != null ? i.getMaterial().getUnidadMedida() : "ud",
                        i.getPrecioUnitario(), i.getSubtotal()))
                .toList();
        String logo = empresa != null && empresa.getLogoImagen() != null
                ? Base64.getEncoder().encodeToString(empresa.getLogoImagen()) : null;
        var labels = condicionesService.listarDisponibles().stream()
                .collect(java.util.stream.Collectors.toMap(
                        com.appgestion.api.dto.response.PresupuestoCondicionDisponibleResponse::clave,
                        com.appgestion.api.dto.response.PresupuestoCondicionDisponibleResponse::textoVisible));
        List<String> condiciones = condicionesService.desdeJson(presupuesto.getCondicionesActivasJson()).stream()
                .map(key -> labels.getOrDefault(key, key))
                .toList();
        return new PresupuestoPublicoResponse(empresa != null ? empresa.getNombre() : null, logo,
                empresa != null && empresa.getLogoImagen() != null ? logoMimeType(empresa.getLogoImagen()) : null,
                presupuesto.getId(),
                presupuesto.getFechaCreacion(), presupuesto.getCliente().getNombre(), items, presupuesto.getSubtotal(),
                presupuesto.getIva(), presupuesto.getTotal(), presupuesto.getNotaAdicional(), condiciones);
    }

    @Transactional(readOnly = true)
    public byte[] getPdf(String token) {
        PresupuestoEnlace enlace = resolve(token);
        Presupuesto presupuesto = enlace.getPresupuesto();
        return pdfService.generarPdf(presupuesto, presupuesto.getUsuario().getId());
    }

    @Transactional
    public void registerView(String token) {
        PresupuestoEnlace enlace = resolve(token);
        Instant now = Instant.now();
        if (enlaceRepository.markFirstView(enlace.getId(), now) == 1) {
            if (presupuestoRepository.markLinkViewNotificationSent(enlace.getPresupuesto().getId()) == 1) {
                notificacionService.presupuestoVisto(enlace.getPresupuesto().getUsuario(),
                        enlace.getPresupuesto().getCliente().getNombre(), enlace.getPresupuesto().getId());
            }
        } else {
            enlaceRepository.countSubsequentView(enlace.getId(), now, now.minus(1, ChronoUnit.MINUTES));
        }
    }

    public static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    private static String logoMimeType(byte[] image) {
        if (image.length >= 12 && image[0] == 'R' && image[1] == 'I' && image[2] == 'F' && image[3] == 'F'
                && image[8] == 'W' && image[9] == 'E' && image[10] == 'B' && image[11] == 'P') {
            return "image/webp";
        }
        if (image.length >= 3 && (image[0] & 0xff) == 0xff && (image[1] & 0xff) == 0xd8 && (image[2] & 0xff) == 0xff) {
            return "image/jpeg";
        }
        return "image/png";
    }

    private PresupuestoEnlace resolve(String token) {
        try {
            return enlaceRepository.findByTokenHashAndRevocadoFalseAndExpiraAtAfter(hash(token), Instant.now())
                    .orElseThrow(() -> unavailable());
        } catch (IllegalArgumentException ex) {
            throw unavailable();
        }
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, UNAVAILABLE);
    }
}
