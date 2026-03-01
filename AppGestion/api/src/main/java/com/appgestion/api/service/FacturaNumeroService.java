package com.appgestion.api.service;

import com.appgestion.api.domain.entity.FacturaSecuencia;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.repository.FacturaRepository;
import com.appgestion.api.repository.FacturaSecuenciaRepository;
import com.appgestion.api.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.time.Year;

@Service
public class FacturaNumeroService {

    private static final String SERIE_DEFAULT = "FAC";

    private final FacturaSecuenciaRepository facturaSecuenciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final FacturaRepository facturaRepository;
    private final EntityManager entityManager;

    public FacturaNumeroService(FacturaSecuenciaRepository facturaSecuenciaRepository,
                                UsuarioRepository usuarioRepository,
                                FacturaRepository facturaRepository,
                                EntityManager entityManager) {
        this.facturaSecuenciaRepository = facturaSecuenciaRepository;
        this.usuarioRepository = usuarioRepository;
        this.facturaRepository = facturaRepository;
        this.entityManager = entityManager;
    }

    /**
     * Genera el siguiente número de factura correlativo para el usuario.
     * Formato: FAC-YYYY-NNNN (ej. FAC-2026-0008)
     * Usa bloqueo pesimista para garantizar correlatividad en concurrencia.
     */
    @Transactional
    public String generarSiguienteNumero(Long usuarioId) {
        Long id = Objects.requireNonNull(usuarioId, "usuarioId no puede ser null");
        int anio = Year.now().getValue();

        FacturaSecuencia sec = facturaSecuenciaRepository.findByUsuarioIdForUpdate(id)
                .orElseGet(() -> crearSecuencia(id, anio));

        if (sec.getAnio() != anio) {
            sec.setAnio(anio);
            sec.setUltimoNumero(0);
        }

        int siguiente = sec.getUltimoNumero() + 1;
        sec.setUltimoNumero(siguiente);
        facturaSecuenciaRepository.save(sec);

        return String.format("%s-%d-%04d", sec.getSerie(), anio, siguiente);
    }

    /**
     * Crea una nueva secuencia para el usuario. Usa persist() explícitamente
     * porque con @MapsId la entidad tiene ID asignado y save() intentaría merge(),
     * causando "unsaved-value mapping was incorrect" si la fila no existe.
     */
    private FacturaSecuencia crearSecuencia(Long usuarioId, int anio) {
        Long id = Objects.requireNonNull(usuarioId, "usuarioId no puede ser null");
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + id));
        int ultimoExistente = facturaRepository.findMaxNumeroInYear(id, SERIE_DEFAULT + "-" + anio + "-%");
        FacturaSecuencia sec = new FacturaSecuencia();
        sec.setUsuario(usuario);
        sec.setSerie(SERIE_DEFAULT);
        sec.setAnio(anio);
        sec.setUltimoNumero(ultimoExistente);
        entityManager.persist(sec);
        entityManager.flush();
        return sec;
    }
}
