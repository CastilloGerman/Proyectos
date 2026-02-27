package com.appgestion.api.repository;

import com.appgestion.api.domain.entity.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {

    List<Factura> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    @Query(value = "SELECT * FROM facturas WHERE id = :id AND usuario_id = :usuarioId", nativeQuery = true)
    Optional<Factura> findByIdAndUsuarioId(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    boolean existsByIdAndUsuarioId(Long id, Long usuarioId);

    Optional<Factura> findByNumeroFacturaAndUsuarioId(String numeroFactura, Long usuarioId);

    long countByUsuarioId(Long usuarioId);

    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(numero_factura FROM '[0-9]+$') AS INTEGER)), 0) FROM facturas WHERE usuario_id = :usuarioId AND numero_factura LIKE :pattern", nativeQuery = true)
    int findMaxNumeroInYear(@Param("usuarioId") Long usuarioId, @Param("pattern") String pattern);
}
