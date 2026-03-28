package com.appgestion.api.repository;

import com.appgestion.api.domain.entity.EmailJob;
import com.appgestion.api.domain.enums.EmailJobStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface EmailJobRepository extends JpaRepository<EmailJob, Long> {

    @Query("""
            SELECT j FROM EmailJob j
            WHERE j.status = :pending
            AND (j.nextRetryAt IS NULL OR j.nextRetryAt <= :now)
            ORDER BY j.createdAt ASC
            """)
    List<EmailJob> findDuePending(@Param("pending") EmailJobStatus pending, @Param("now") Instant now, Pageable pageable);
}
