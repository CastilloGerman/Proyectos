package com.appgestion.api.service;

import com.appgestion.api.domain.entity.EmailJob;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.EmailJobStatus;
import com.appgestion.api.repository.EmailJobRepository;
import com.appgestion.api.service.email.EmailDispatchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;

@Service
public class EmailJobProcessor {

    private final EmailJobRepository emailJobRepository;
    private final EmailDispatchService emailDispatchService;
    private final TransactionTemplate readTx;
    private final TransactionTemplate writeTx;

    public EmailJobProcessor(
            EmailJobRepository emailJobRepository,
            EmailDispatchService emailDispatchService,
            PlatformTransactionManager transactionManager) {
        this.emailJobRepository = emailJobRepository;
        this.emailDispatchService = emailDispatchService;
        this.readTx = new TransactionTemplate(transactionManager);
        this.readTx.setReadOnly(true);
        this.writeTx = new TransactionTemplate(transactionManager);
    }

    /**
     * El envío (Resend, Gmail, SMTP) queda fuera de la transacción. Si no, la conexión
     * del pool se queda ocupada durante toda la llamada de red y las lecturas del login esperan.
     */
    public void process(Long jobId) {
        EmailJob job = readTx.execute(status -> loadPending(jobId));
        if (job == null) {
            return;
        }
        try {
            emailDispatchService.dispatch(job);
            writeTx.executeWithoutResult(status -> markSent(jobId));
        } catch (Exception e) {
            String msg = e.getMessage();
            writeTx.executeWithoutResult(status -> markFailed(jobId, msg));
        }
    }

    private EmailJob loadPending(Long jobId) {
        EmailJob found = emailJobRepository.findById(jobId).orElse(null);
        if (found == null || found.getStatus() != EmailJobStatus.PENDING) {
            return null;
        }
        Usuario usuario = found.getUsuario();
        if (usuario != null) {
            usuario.getNombre();
        }
        return found;
    }

    private void markSent(Long jobId) {
        EmailJob job = emailJobRepository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() != EmailJobStatus.PENDING) {
            return;
        }
        job.setStatus(EmailJobStatus.SENT);
        job.setProcessedAt(Instant.now());
        job.setLastError(null);
        emailJobRepository.save(job);
    }

    private void markFailed(Long jobId, String message) {
        EmailJob job = emailJobRepository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() != EmailJobStatus.PENDING) {
            return;
        }
        job.setAttempts(job.getAttempts() + 1);
        job.setLastError(message != null && message.length() > 4000 ? message.substring(0, 4000) : message);
        if (job.getAttempts() >= job.getMaxAttempts()) {
            job.setStatus(EmailJobStatus.DEAD);
            job.setProcessedAt(Instant.now());
        } else {
            long minutes = Math.min(60, 1L << Math.min(job.getAttempts(), 6));
            job.setNextRetryAt(Instant.now().plus(Duration.ofMinutes(minutes)));
        }
        emailJobRepository.save(job);
    }
}
