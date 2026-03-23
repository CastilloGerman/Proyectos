package com.appgestion.api.service;

import com.appgestion.api.repository.AuditAccessEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuditAccessCleanupService {

    private final AuditAccessEventRepository auditAccessEventRepository;

    public AuditAccessCleanupService(AuditAccessEventRepository auditAccessEventRepository) {
        this.auditAccessEventRepository = auditAccessEventRepository;
    }

    @Transactional
    public int deleteOlderThan(Instant cutoff) {
        return auditAccessEventRepository.deleteByOccurredAtBefore(cutoff);
    }
}
