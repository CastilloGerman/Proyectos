package com.appgestion.api.repository;

import com.appgestion.api.domain.entity.FacturaCobro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FacturaCobroRepository extends JpaRepository<FacturaCobro, Long> {

    List<FacturaCobro> findByFacturaIdOrderByFechaDescCreatedAtDesc(Long facturaId);
}
