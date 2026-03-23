package com.appgestion.api.repository;

import com.appgestion.api.domain.entity.AuditAccessEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface AuditAccessEventRepository extends JpaRepository<AuditAccessEvent, Long>, JpaSpecificationExecutor<AuditAccessEvent> {

    @Modifying
    @Query("DELETE FROM AuditAccessEvent e WHERE e.occurredAt < :cutoff")
    int deleteByOccurredAtBefore(@Param("cutoff") Instant cutoff);
}
