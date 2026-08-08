package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Gasto;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.dto.request.GastoRequest;
import com.appgestion.api.dto.response.GastoResponse;
import com.appgestion.api.repository.GastoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class GastoService {

    private final GastoRepository gastoRepository;

    public GastoService(GastoRepository gastoRepository) {
        this.gastoRepository = gastoRepository;
    }

    public List<GastoResponse> listar(Long usuarioId) {
        return gastoRepository.findByUsuarioIdOrderByFechaDesc(usuarioId).stream()
                .map(this::toResponse)
                .toList();
    }

    public GastoResponse obtenerPorId(Long id, Long usuarioId) {
        Gasto gasto = gastoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gasto no encontrado"));
        return toResponse(gasto);
    }

    @Transactional
    public GastoResponse crear(GastoRequest request, Usuario usuario) {
        validarRequest(request);
        Gasto gasto = new Gasto();
        gasto.setUsuario(usuario);
        mapRequestToEntity(request, gasto);
        gasto = Objects.requireNonNull(gastoRepository.save(gasto));
        return toResponse(gasto);
    }

    @Transactional
    public GastoResponse actualizar(Long id, GastoRequest request, Long usuarioId) {
        Gasto gasto = gastoRepository.findByIdAndUsuarioId(
                        Objects.requireNonNull(id), Objects.requireNonNull(usuarioId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gasto no encontrado"));
        validarRequest(request);
        mapRequestToEntity(request, gasto);
        gasto = Objects.requireNonNull(gastoRepository.save(gasto));
        return toResponse(gasto);
    }

    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        if (!gastoRepository.existsByIdAndUsuarioId(Objects.requireNonNull(id), Objects.requireNonNull(usuarioId))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Gasto no encontrado");
        }
        gastoRepository.deleteById(id);
    }

    public static double calcularCuotaIva(double baseImponible, double tipoIva) {
        return BigDecimal.valueOf(baseImponible)
                .multiply(BigDecimal.valueOf(tipoIva))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private void validarRequest(GastoRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos del gasto obligatorios");
        }
        if (!StringUtils.hasText(request.proveedor())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El proveedor es obligatorio");
        }
        if (!StringUtils.hasText(request.concepto())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El concepto es obligatorio");
        }
        if (request.fecha() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha es obligatoria");
        }
        if (request.fecha().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha no puede ser futura");
        }
        if (request.baseImponible() == null || request.baseImponible() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La base imponible debe ser mayor o igual a 0");
        }
        if (request.tipoIva() == null || request.tipoIva() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tipo de IVA debe ser mayor o igual a 0");
        }
        if (request.categoria() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría es obligatoria");
        }
    }

    private void mapRequestToEntity(GastoRequest request, Gasto gasto) {
        gasto.setProveedor(request.proveedor().trim());
        gasto.setConcepto(request.concepto().trim());
        gasto.setFecha(request.fecha());
        gasto.setBaseImponible(request.baseImponible());
        gasto.setTipoIva(request.tipoIva());
        gasto.setCuotaIva(calcularCuotaIva(request.baseImponible(), request.tipoIva()));
        gasto.setCategoria(request.categoria());
    }

    private GastoResponse toResponse(Gasto gasto) {
        return new GastoResponse(
                gasto.getId(),
                gasto.getProveedor(),
                gasto.getConcepto(),
                gasto.getFecha(),
                gasto.getBaseImponible(),
                gasto.getTipoIva(),
                gasto.getCuotaIva(),
                gasto.getCategoria()
        );
    }
}
