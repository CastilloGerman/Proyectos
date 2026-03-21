package com.appgestion.api.repository;

import com.appgestion.api.domain.entity.FacturaCobro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacturaCobroRepository extends JpaRepository<FacturaCobro, Long> {

    List<FacturaCobro> findByFacturaIdOrderByFechaDescCreatedAtDesc(Long facturaId);
}
