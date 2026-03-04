package com.appgestion.api.scheduler;

import com.appgestion.api.service.FacturaRecordatorioService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Envía recordatorios diarios de cobro para facturas vencidas o próximas a vencer.
 * Solo se ejecuta si el usuario tiene correo SMTP configurado en su empresa.
 */
@Component
public class FacturaRecordatorioJob {

    private final FacturaRecordatorioService facturaRecordatorioService;

    public FacturaRecordatorioJob(FacturaRecordatorioService facturaRecordatorioService) {
        this.facturaRecordatorioService = facturaRecordatorioService;
    }

    @Scheduled(cron = "0 0 8 * * ?")
    public void enviarRecordatorios() {
        facturaRecordatorioService.enviarRecordatorios();
    }
}
