package com.appgestion.api.service;

import com.appgestion.api.catalog.RubroAutonomoCatalog;
import com.appgestion.api.domain.entity.Empresa;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.EmailProviderMode;
import com.appgestion.api.domain.enums.OAuthOnFailureMode;
import com.appgestion.api.dto.request.EmpresaRequest;
import com.appgestion.api.dto.request.DatosFiscalesPatchRequest;
import com.appgestion.api.dto.request.MetodosCobroPatchRequest;
import com.appgestion.api.dto.request.PlantillasPdfPatchRequest;
import com.appgestion.api.dto.request.RecordatorioCobroPatchRequest;
import com.appgestion.api.dto.response.EmpresaResponse;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.util.BizumTelefonoValidator;
import com.appgestion.api.util.IbanValidator;
import com.appgestion.api.util.NifIvaIntraValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EmpresaService {

    private static final int MAX_LOGO_BYTES = 400_000;

    private static final Set<String> METODOS_PAGO_PERMITIDOS = Set.of("Transferencia", "Efectivo", "Tarjeta", "Bizum");
    private static final Set<Integer> DIAS_RECORDATORIO_CLIENTE_PERMITIDOS = Set.of(7, 15, 30);

    private final EmpresaRepository empresaRepository;

    public EmpresaService(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    public EmpresaResponse obtenerPorUsuario(Long usuarioId) {
        Empresa emp = empresaRepository.findByUsuarioId(usuarioId).orElse(null);
        return toResponse(emp);
    }

    @Transactional
    public EmpresaResponse guardar(EmpresaRequest request, Usuario usuario) {
        Empresa emp = empresaRepository.findByUsuarioId(usuario.getId())
                .orElseGet(() -> {
                    Empresa e = new Empresa();
                    e.setUsuario(usuario);
                    return e;
                });
        emp.setNombre(request.nombre() != null ? request.nombre() : "");
        emp.setDireccion(request.direccion());
        emp.setCodigoPostal(request.codigoPostal());
        emp.setProvincia(request.provincia());
        emp.setPais(request.pais() != null ? request.pais() : "España");
        emp.setNif(request.nif());
        emp.setTelefono(request.telefono());
        emp.setEmail(request.email());
        emp.setNotasPiePresupuesto(request.notasPiePresupuesto());
        emp.setNotasPieFactura(request.notasPieFactura());
        if (request.mailHost() != null) emp.setMailHost(request.mailHost());
        if (request.mailPort() != null) emp.setMailPort(request.mailPort());
        if (request.mailUsername() != null) emp.setMailUsername(request.mailUsername());
        if (request.mailPassword() != null && !request.mailPassword().isBlank()) emp.setMailPassword(request.mailPassword());
        if (request.logoImagenBase64() != null) {
            if (request.logoImagenBase64().isBlank()) {
                emp.setLogoImagen(null);
            } else {
                String raw = request.logoImagenBase64().trim();
                int comma = raw.indexOf(',');
                if (raw.startsWith("data:") && comma > 0) {
                    raw = raw.substring(comma + 1);
                }
                try {
                    byte[] decoded = Base64.getDecoder().decode(raw);
                    if (decoded.length > MAX_LOGO_BYTES) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "El logo no puede superar " + (MAX_LOGO_BYTES / 1024) + " KB");
                    }
                    emp.setLogoImagen(decoded);
                } catch (IllegalArgumentException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Logo en Base64 no válido");
                }
            }
        }
        if (request.rubroAutonomoCodigo() != null) {
            String rubro = request.rubroAutonomoCodigo().trim();
            if (rubro.isEmpty()) {
                emp.setRubroAutonomoCodigo(null);
            } else if (!RubroAutonomoCatalog.esCodigoValido(rubro)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rubro o actividad no reconocido");
            } else {
                emp.setRubroAutonomoCodigo(rubro);
            }
        }
        if (request.emailProvider() != null && !request.emailProvider().isBlank()) {
            try {
                emp.setEmailProvider(EmailProviderMode.valueOf(request.emailProvider().trim()));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Modo de envío de correo no válido");
            }
        }
        if (request.oauthOnFailure() != null && !request.oauthOnFailure().isBlank()) {
            try {
                emp.setOauthOnFailure(OAuthOnFailureMode.valueOf(request.oauthOnFailure().trim()));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Política ante fallo OAuth no válida");
            }
        }
        if (request.systemFromOverride() != null) {
            String sfo = request.systemFromOverride().trim();
            emp.setSystemFromOverride(sfo.isEmpty() ? null : sfo);
        }
        emp = empresaRepository.save(emp);
        return toResponse(emp);
    }

    @Transactional
    public EmpresaResponse actualizarMetodosCobro(MetodosCobroPatchRequest request, Usuario usuario) {
        String metodo = request.defaultMetodoPago() != null ? request.defaultMetodoPago().trim() : "";
        if (!metodo.isEmpty() && !METODOS_PAGO_PERMITIDOS.contains(metodo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Método de pago no reconocido");
        }
        String ibanRaw = request.ibanCuenta() != null ? IbanValidator.normalize(request.ibanCuenta()) : "";
        if (!ibanRaw.isEmpty() && !IbanValidator.isValid(ibanRaw)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IBAN no válido");
        }
        String bizumRaw = request.bizumTelefono() != null ? request.bizumTelefono().trim() : "";
        if (!bizumRaw.isEmpty() && !BizumTelefonoValidator.isValid(bizumRaw)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Teléfono Bizum no válido (móvil español, 9 dígitos)");
        }
        String cond = request.defaultCondicionesPago() != null ? request.defaultCondicionesPago().trim() : "";
        if (cond.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Condiciones de pago demasiado largas");
        }

        Empresa emp = empresaRepository.findByUsuarioId(usuario.getId()).orElseGet(() -> {
            Empresa e = new Empresa();
            e.setUsuario(usuario);
            String n = usuario.getNombre();
            e.setNombre(n != null && !n.isBlank() ? n : "");
            return e;
        });
        emp.setDefaultMetodoPago(metodo.isEmpty() ? null : metodo);
        emp.setDefaultCondicionesPago(cond.isEmpty() ? null : cond);
        emp.setIbanCuenta(ibanRaw.isEmpty() ? null : ibanRaw.toUpperCase());
        if (request.nombreBanco() != null) {
            emp.setNombreBanco(blankToNull(request.nombreBanco()));
        }
        if (request.titularCuenta() != null) {
            emp.setTitularCuenta(blankToNull(request.titularCuenta()));
        }
        emp.setBizumTelefono(bizumRaw.isEmpty() ? null : normalizeBizumDigits(bizumRaw));
        emp = empresaRepository.save(emp);
        return toResponse(emp);
    }

    private static String normalizeBizumDigits(String raw) {
        String digits = raw.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+34")) {
            digits = digits.substring(3);
        } else if (digits.startsWith("0034")) {
            digits = digits.substring(4);
        }
        digits = digits.replace("+", "");
        return digits;
    }

    @Transactional
    public EmpresaResponse actualizarPlantillasPdf(PlantillasPdfPatchRequest request, Usuario usuario) {
        Empresa emp = empresaRepository.findByUsuarioId(usuario.getId()).orElseGet(() -> {
            Empresa e = new Empresa();
            e.setUsuario(usuario);
            String n = usuario.getNombre();
            e.setNombre(n != null && !n.isBlank() ? n : "");
            return e;
        });
        if (request.notasPiePresupuesto() != null) {
            emp.setNotasPiePresupuesto(blankToNull(request.notasPiePresupuesto()));
        }
        if (request.notasPieFactura() != null) {
            emp.setNotasPieFactura(blankToNull(request.notasPieFactura()));
        }
        Empresa guardada = Objects.requireNonNull(empresaRepository.save(Objects.requireNonNull(emp)));
        return toResponse(guardada);
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    @Transactional
    public EmpresaResponse actualizarDatosFiscales(DatosFiscalesPatchRequest request, Usuario usuario) {
        String regimen = request.regimenIvaPrincipal() != null ? request.regimenIvaPrincipal().trim() : "";
        if (regimen.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica el régimen de IVA o impuesto aplicable");
        }
        if (regimen.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Régimen fiscal demasiado largo");
        }
        String nifIva = request.nifIntracomunitario() != null
                ? request.nifIntracomunitario().trim().replaceAll("\\s+", "").toUpperCase() : "";
        if (!nifIva.isEmpty() && !NifIvaIntraValidator.isValid(nifIva)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "NIF-IVA intracomunitario no válido (formato tipo ESB12345678)");
        }
        String desc = request.descripcionActividad() != null ? request.descripcionActividad().trim() : "";
        if (desc.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Descripción de actividad demasiado larga");
        }
        String ep = request.epigrafeIae() != null ? request.epigrafeIae().trim() : "";
        if (ep.length() > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Epígrafe IAE demasiado largo");
        }

        Empresa emp = empresaRepository.findByUsuarioId(usuario.getId()).orElseGet(() -> {
            Empresa e = new Empresa();
            e.setUsuario(usuario);
            String n = usuario.getNombre();
            e.setNombre(n != null && !n.isBlank() ? n : "");
            return e;
        });
        emp.setRegimenIvaPrincipal(regimen);
        emp.setDescripcionActividadFiscal(desc.isEmpty() ? null : desc);
        emp.setNifIntracomunitario(nifIva.isEmpty() ? null : nifIva);
        emp.setEpigrafeIae(ep.isEmpty() ? null : ep);
        emp = empresaRepository.save(emp);
        return toResponse(emp);
    }

    @Transactional
    public EmpresaResponse actualizarRecordatoriosCobro(RecordatorioCobroPatchRequest request, Usuario usuario) {
        List<Integer> diasFiltrados = request.recordatorioClienteDias().stream()
                .filter(d -> d != null && DIAS_RECORDATORIO_CLIENTE_PERMITIDOS.contains(d))
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
        if (Boolean.TRUE.equals(request.recordatorioClienteActivo()) && diasFiltrados.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Selecciona al menos un plazo de recordatorio (7, 15 o 30 días tras el vencimiento)");
        }
        Empresa emp = empresaRepository.findByUsuarioId(usuario.getId()).orElseGet(() -> {
            Empresa e = new Empresa();
            e.setUsuario(usuario);
            String n = usuario.getNombre();
            e.setNombre(n != null && !n.isBlank() ? n : "");
            return e;
        });
        emp.setRecordatorioClienteActivo(request.recordatorioClienteActivo());
        if (diasFiltrados.isEmpty()) {
            emp.setRecordatorioClienteDias("7,15,30");
        } else {
            emp.setRecordatorioClienteDias(diasFiltrados.stream().map(String::valueOf).collect(Collectors.joining(",")));
        }
        emp = empresaRepository.save(emp);
        return toResponse(emp);
    }

    public Empresa getEmpresaOrNull(Long usuarioId) {
        return empresaRepository.findByUsuarioId(usuarioId).orElse(null);
    }

    private EmpresaResponse toResponse(Empresa emp) {
        if (emp == null) {
            return new EmpresaResponse(
                    null,
                    "",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    false,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    List.of(7, 15, 30),
                    EmailProviderMode.system.name(),
                    null,
                    false,
                    null,
                    OAuthOnFailureMode.system.name(),
                    null);
        }
        boolean mailConfigurado = emp.getMailUsername() != null && !emp.getMailUsername().isBlank()
                && emp.getMailPassword() != null && !emp.getMailPassword().isBlank();
        byte[] logo = emp.getLogoImagen();
        boolean tieneLogo = logo != null && logo.length > 0;
        String logoB64 = tieneLogo ? Base64.getEncoder().encodeToString(logo) : null;
        return new EmpresaResponse(
                emp.getId(),
                emp.getNombre(),
                emp.getDireccion(),
                emp.getCodigoPostal(),
                emp.getProvincia(),
                emp.getPais(),
                emp.getNif(),
                emp.getTelefono(),
                emp.getEmail(),
                emp.getNotasPiePresupuesto(),
                emp.getNotasPieFactura(),
                emp.getMailHost(),
                emp.getMailPort(),
                emp.getMailUsername(),
                mailConfigurado,
                tieneLogo,
                logoB64,
                emp.getDefaultMetodoPago(),
                emp.getDefaultCondicionesPago(),
                emp.getIbanCuenta(),
                emp.getNombreBanco(),
                emp.getTitularCuenta(),
                emp.getBizumTelefono(),
                emp.getRegimenIvaPrincipal(),
                emp.getDescripcionActividadFiscal(),
                emp.getNifIntracomunitario(),
                emp.getEpigrafeIae(),
                emp.getRubroAutonomoCodigo(),
                emp.getRecordatorioClienteActivo() != null && emp.getRecordatorioClienteActivo(),
                parseDiasRecordatorioLista(emp.getRecordatorioClienteDias()),
                emp.getEmailProvider() != null ? emp.getEmailProvider().name() : EmailProviderMode.system.name(),
                emp.getOauthProvider(),
                StringUtils.hasText(emp.getOauthRefreshTokenEnc()),
                emp.getOauthConnectedAt(),
                emp.getOauthOnFailure() != null ? emp.getOauthOnFailure().name() : OAuthOnFailureMode.system.name(),
                emp.getSystemFromOverride()
        );
    }

    private static List<Integer> parseDiasRecordatorioLista(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of(7, 15, 30);
        }
        List<Integer> out = new ArrayList<>();
        for (String part : raw.split(",")) {
            String t = part.trim();
            if (t.isEmpty()) {
                continue;
            }
            try {
                int d = Integer.parseInt(t);
                if (DIAS_RECORDATORIO_CLIENTE_PERMITIDOS.contains(d))
                    out.add(d);
            } catch (NumberFormatException ignored) {
                // omitir token inválido
            }
        }
        out.sort(Comparator.naturalOrder());
        return out.isEmpty() ? List.of(7, 15, 30) : List.copyOf(out);
    }
}
