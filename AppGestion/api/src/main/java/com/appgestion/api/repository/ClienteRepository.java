package com.appgestion.api.repository;

import com.appgestion.api.domain.entity.Cliente;
import com.appgestion.api.domain.enums.EstadoCliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    List<Cliente> findByUsuarioId(Long usuarioId);

    List<Cliente> findByUsuarioIdAndEstadoCliente(Long usuarioId, EstadoCliente estadoCliente);

    long countByUsuarioIdAndEstadoCliente(Long usuarioId, EstadoCliente estadoCliente);

    Optional<Cliente> findByIdAndUsuarioId(Long id, Long usuarioId);

    boolean existsByIdAndUsuarioId(Long id, Long usuarioId);

    @Query("""
            SELECT COUNT(c) > 0
            FROM Cliente c
            WHERE c.usuario.id = :usuarioId
              AND c.dni IS NOT NULL
              AND TRIM(c.dni) <> ''
              AND REPLACE(REPLACE(REPLACE(LOWER(TRIM(c.dni)), ' ', ''), '-', ''), '.', '') = :dniNormalizado
            """)
    boolean existsDniDuplicado(@Param("usuarioId") Long usuarioId, @Param("dniNormalizado") String dniNormalizado);

    @Query("""
            SELECT COUNT(c) > 0
            FROM Cliente c
            WHERE c.usuario.id = :usuarioId
              AND c.id <> :clienteId
              AND c.dni IS NOT NULL
              AND TRIM(c.dni) <> ''
              AND REPLACE(REPLACE(REPLACE(LOWER(TRIM(c.dni)), ' ', ''), '-', ''), '.', '') = :dniNormalizado
            """)
    boolean existsDniDuplicadoExcluyendoCliente(
            @Param("usuarioId") Long usuarioId,
            @Param("clienteId") Long clienteId,
            @Param("dniNormalizado") String dniNormalizado);
}
