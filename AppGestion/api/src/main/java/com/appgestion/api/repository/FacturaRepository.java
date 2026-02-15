package com.appgestion.api.repository;

import com.appgestion.api.domain.entity.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {

    List<Factura> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    Optional<Factura> findByIdAndUsuarioId(Long id, Long usuarioId);

    boolean existsByIdAndUsuarioId(Long id, Long usuarioId);

    Optional<Factura> findByNumeroFacturaAndUsuarioId(String numeroFactura, Long usuarioId);

    long countByUsuarioId(Long usuarioId);
}
