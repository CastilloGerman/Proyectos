package com.appgestion.api.service;

import com.appgestion.api.dto.FacturaNumeroGenerado;
import com.appgestion.api.domain.entity.FacturaSecuencia;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.repository.FacturaRepository;
import com.appgestion.api.repository.FacturaSecuenciaRepository;
import com.appgestion.api.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

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
     * Genera el siguiente número correlativo para el año indicado (bloqueo pesimista en {@code factura_secuencia}).
     * Formato: FAC-YYYY-NNNN.
     */
    @Transactional
    public FacturaNumeroGenerado generarSiguienteNumeroFactura(Long usuarioId, int anio) {
        Long id = Objects.requireNonNull(usuarioId, "usuarioId no puede ser null");

        FacturaSecuencia sec = facturaSecuenciaRepository.findByUsuarioIdForUpdate(id)
                .orElseGet(() -> crearSecuencia(id, anio));

        if (!Objects.equals(sec.getAnio(), anio)) {
            sec.setAnio(anio);
            int maxFromDb = facturaRepository.maxNumeroSecuencialByUsuarioAndAnio(id, anio);
            sec.setUltimoNumero(maxFromDb);
        }

        int maxDb = facturaRepository.maxNumeroSecuencialByUsuarioAndAnio(id, anio);
        if (sec.getUltimoNumero() < maxDb) {
            sec.setUltimoNumero(maxDb);
        }

        int siguiente = sec.getUltimoNumero() + 1;
        sec.setUltimoNumero(siguiente);
        facturaSecuenciaRepository.save(sec);

        String numero = String.format("%s-%d-%04d", sec.getSerie(), anio, siguiente);
        return new FacturaNumeroGenerado(numero, anio, siguiente);
    }

    /**
     * Compatibilidad: año actual del calendario.
     */
    @Transactional
    public String generarSiguienteNumero(Long usuarioId) {
        int anio = java.time.Year.now().getValue();
        return generarSiguienteNumeroFactura(usuarioId, anio).numeroFactura();
    }

    private FacturaSecuencia crearSecuencia(Long usuarioId, int anio) {
        Long id = Objects.requireNonNull(usuarioId, "usuarioId no puede ser null");
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + id));
        int ultimoExistente = facturaRepository.maxNumeroSecuencialByUsuarioAndAnio(id, anio);
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
