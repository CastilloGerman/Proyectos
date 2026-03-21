package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Factura;
import com.appgestion.api.domain.entity.FacturaCobro;
import com.appgestion.api.dto.request.FacturaCobroRequest;
import com.appgestion.api.dto.response.FacturaResponse;
import com.appgestion.api.repository.FacturaCobroRepository;
import com.appgestion.api.repository.FacturaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class FacturaCobroService {

    private final FacturaRepository facturaRepository;
    private final FacturaCobroRepository facturaCobroRepository;
    private final FacturaResponseMapper facturaResponseMapper;

    public FacturaCobroService(FacturaRepository facturaRepository,
                               FacturaCobroRepository facturaCobroRepository,
                               FacturaResponseMapper facturaResponseMapper) {
        this.facturaRepository = facturaRepository;
        this.facturaCobroRepository = facturaCobroRepository;
        this.facturaResponseMapper = facturaResponseMapper;
    }

    @Transactional
    public FacturaResponse registrarCobro(Long facturaId, Long usuarioId, FacturaCobroRequest request) {
        Factura factura = facturaRepository.findByIdAndUsuarioId(facturaId, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada"));
        FacturaCobro cobro = new FacturaCobro();
        cobro.setFactura(factura);
        cobro.setImporte(request.importe());
        cobro.setFecha(request.fecha());
        cobro.setMetodo(request.metodo());
        cobro.setNotas(request.notas());
        facturaCobroRepository.save(cobro);

        double prev = Optional.ofNullable(factura.getMontoCobrado()).orElse(0.0);
        factura.setMontoCobrado(prev + request.importe());
        double total = Optional.ofNullable(factura.getTotal()).orElse(0.0);
        if (factura.getMontoCobrado() + 0.001 >= total) {
            factura.setEstadoPago("Pagada");
        } else if (factura.getMontoCobrado() > 0) {
            factura.setEstadoPago("Parcial");
        }
        facturaRepository.save(factura);
        return facturaResponseMapper.toResponse(factura,
                facturaCobroRepository.findByFacturaIdOrderByFechaDescCreatedAtDesc(factura.getId()));
    }
}
