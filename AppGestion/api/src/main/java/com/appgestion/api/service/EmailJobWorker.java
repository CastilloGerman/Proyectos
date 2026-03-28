package com.appgestion.api.service;

import com.appgestion.api.domain.entity.EmailJob;
import com.appgestion.api.domain.enums.EmailJobStatus;
import com.appgestion.api.repository.EmailJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class EmailJobWorker {

    private static final Logger log = LoggerFactory.getLogger(EmailJobWorker.class);

    private final EmailJobRepository emailJobRepository;
    private final EmailJobProcessor emailJobProcessor;
    public EmailJobWorker(
            EmailJobRepository emailJobRepository,
            EmailJobProcessor emailJobProcessor) {
        this.emailJobRepository = emailJobRepository;
        this.emailJobProcessor = emailJobProcessor;
    }

    @Scheduled(fixedDelayString = "${app.email.worker-delay-ms:3000}")
    public void poll() {
        Instant now = Instant.now();
        List<EmailJob> batch = emailJobRepository.findDuePending(EmailJobStatus.PENDING, now, PageRequest.of(0, 25));
        for (EmailJob job : batch) {
            try {
                emailJobProcessor.process(job.getId());
            } catch (Exception e) {
                log.warn("Worker email job {} error: {}", job.getId(), e.getMessage());
            }
        }
    }
}
