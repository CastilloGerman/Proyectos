package com.appgestion.api.scheduler;

import com.appgestion.api.service.FacturaRecordatorioClienteService;
import com.appgestion.api.service.FacturaRecordatorioService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tarea diaria (08:00): encola recordatorios al autónomo (vencimiento próximo) y, si está activado en empresa,
 * recordatorios al cliente (7 / 15 / 30 días tras el vencimiento). El envío real lo hace el worker
 * (modo system / Resend, OAuth o SMTP legacy según {@code empresas.email_provider}).
 */
@Component
public class FacturaRecordatorioJob {

    private final FacturaRecordatorioService facturaRecordatorioService;
    private final FacturaRecordatorioClienteService facturaRecordatorioClienteService;

    public FacturaRecordatorioJob(
            FacturaRecordatorioService facturaRecordatorioService,
            FacturaRecordatorioClienteService facturaRecordatorioClienteService) {
        this.facturaRecordatorioService = facturaRecordatorioService;
        this.facturaRecordatorioClienteService = facturaRecordatorioClienteService;
    }

    @Scheduled(cron = "0 0 8 * * ?")
    public void enviarRecordatorios() {
        facturaRecordatorioService.enviarRecordatorios();
        facturaRecordatorioClienteService.enviarRecordatorios();
    }
}
