package com.appgestion.api.repository;

import com.appgestion.api.domain.entity.Cliente;
import com.appgestion.api.domain.enums.EstadoCliente;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
