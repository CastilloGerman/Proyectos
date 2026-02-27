package com.appgestion.api.repository;

import com.appgestion.api.domain.entity.FacturaSecuencia;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FacturaSecuenciaRepository extends JpaRepository<FacturaSecuencia, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT fs FROM FacturaSecuencia fs WHERE fs.usuarioId = :usuarioId")
    Optional<FacturaSecuencia> findByUsuarioIdForUpdate(@Param("usuarioId") Long usuarioId);
}
