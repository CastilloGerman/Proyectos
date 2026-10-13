package com.appgestion.api.repository;

import com.appgestion.api.domain.entity.FacturaAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacturaAuditoriaRepository extends JpaRepository<FacturaAuditoria, Long> {

    Optional<FacturaAuditoria> findTopByFacturaIdOrderByIdDesc(Long facturaId);

    List<FacturaAuditoria> findByFacturaIdOrderByIdAsc(Long facturaId);
}
