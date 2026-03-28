package com.appgestion.api.service;

import com.appgestion.api.domain.entity.EmailJob;
import com.appgestion.api.domain.enums.EmailJobStatus;
import com.appgestion.api.repository.EmailJobRepository;
import com.appgestion.api.service.email.EmailDispatchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class EmailJobProcessor {

    private final EmailJobRepository emailJobRepository;
    private final EmailDispatchService emailDispatchService;

    public EmailJobProcessor(EmailJobRepository emailJobRepository, EmailDispatchService emailDispatchService) {
        this.emailJobRepository = emailJobRepository;
        this.emailDispatchService = emailDispatchService;
    }

    @Transactional
    public void process(Long jobId) {
        EmailJob job = emailJobRepository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() != EmailJobStatus.PENDING) {
            return;
        }
        try {
            emailDispatchService.dispatch(job);
            job.setStatus(EmailJobStatus.SENT);
            job.setProcessedAt(Instant.now());
            job.setLastError(null);
        } catch (Exception e) {
            job.setAttempts(job.getAttempts() + 1);
            String msg = e.getMessage();
            job.setLastError(msg != null && msg.length() > 4000 ? msg.substring(0, 4000) : msg);
            if (job.getAttempts() >= job.getMaxAttempts()) {
                job.setStatus(EmailJobStatus.DEAD);
                job.setProcessedAt(Instant.now());
            } else {
                long minutes = Math.min(60, 1L << Math.min(job.getAttempts(), 6));
                job.setNextRetryAt(Instant.now().plus(Duration.ofMinutes(minutes)));
            }
        }
        emailJobRepository.save(job);
    }
}
