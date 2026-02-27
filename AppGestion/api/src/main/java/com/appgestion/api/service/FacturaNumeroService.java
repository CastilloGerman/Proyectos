package com.appgestion.api.service;

import com.appgestion.api.domain.entity.FacturaSecuencia;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.repository.FacturaRepository;
import com.appgestion.api.repository.FacturaSecuenciaRepository;
import com.appgestion.api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

@Service
public class FacturaNumeroService {

    private static final String SERIE_DEFAULT = "FAC";

    private final FacturaSecuenciaRepository facturaSecuenciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final FacturaRepository facturaRepository;

    public FacturaNumeroService(FacturaSecuenciaRepository facturaSecuenciaRepository,
                                UsuarioRepository usuarioRepository,
                                FacturaRepository facturaRepository) {
        this.facturaSecuenciaRepository = facturaSecuenciaRepository;
        this.usuarioRepository = usuarioRepository;
        this.facturaRepository = facturaRepository;
    }

    /**
     * Genera el siguiente número de factura correlativo para el usuario.
     * Formato: FAC-YYYY-NNNN (ej. FAC-2026-0008)
     * Usa bloqueo pesimista para garantizar correlatividad en concurrencia.
     */
    @Transactional
    public String generarSiguienteNumero(Long usuarioId) {
        int anio = Year.now().getValue();

        FacturaSecuencia sec = facturaSecuenciaRepository.findByUsuarioIdForUpdate(usuarioId)
                .orElseGet(() -> crearSecuencia(usuarioId, anio));

        if (sec.getAnio() != anio) {
            sec.setAnio(anio);
            sec.setUltimoNumero(0);
        }

        int siguiente = sec.getUltimoNumero() + 1;
        sec.setUltimoNumero(siguiente);
        facturaSecuenciaRepository.save(sec);

        return String.format("%s-%d-%04d", sec.getSerie(), anio, siguiente);
    }

    private FacturaSecuencia crearSecuencia(Long usuarioId, int anio) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + usuarioId));
        int ultimoExistente = facturaRepository.findMaxNumeroInYear(usuarioId, SERIE_DEFAULT + "-" + anio + "-%");
        FacturaSecuencia sec = new FacturaSecuencia();
        sec.setUsuario(usuario);
        sec.setSerie(SERIE_DEFAULT);
        sec.setAnio(anio);
        sec.setUltimoNumero(ultimoExistente);
        return facturaSecuenciaRepository.save(sec);
    }
}
