package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Cliente;
import com.appgestion.api.domain.entity.Presupuesto;
import com.appgestion.api.domain.entity.PresupuestoItem;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.EstadoCliente;
import com.appgestion.api.dto.response.AnticipoResumenDTO;
import com.appgestion.api.repository.FacturaCobroRepository;
import com.appgestion.api.repository.FacturaRepository;
import com.appgestion.api.repository.PresupuestoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnticipoServiceTest {

    private static final long PID = 1L;
    private static final long UID = 99L;

    @Mock
    PresupuestoRepository presupuestoRepository;
    @Mock
    FacturaRepository facturaRepository;
    @Mock
    FacturaNumeroService facturaNumeroService;
    @Mock
    FacturaCobroRepository facturaCobroRepository;
    @Mock
    FacturaResponseMapper facturaResponseMapper;
    @Mock
    FacturaService facturaService;

    @InjectMocks
    AnticipoService anticipoService;

    @Test
    void registrarAnticipo_importeSuperaTotal_lanza400() {
        Presupuesto p = presupuestoAceptadoConLineaIva();
        p.setTotal(100.0);
        when(presupuestoRepository.findByIdAndUsuarioId(PID, UID)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> anticipoService.registrarAnticipo(PID, new BigDecimal("100.01"), LocalDate.now(), UID))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    void registrarAnticipo_estadoNoAceptado_lanza400() {
        Presupuesto p = presupuestoAceptadoConLineaIva();
        p.setEstado("Pendiente");
        when(presupuestoRepository.findByIdAndUsuarioId(PID, UID)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> anticipoService.registrarAnticipo(PID, new BigDecimal("10"), LocalDate.now(), UID))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    void registrarAnticipo_ok_persisteImporteYFecha() {
        Presupuesto p = presupuestoAceptadoConLineaIva();
        p.setTotal(500.0);
        when(presupuestoRepository.findByIdAndUsuarioId(PID, UID)).thenReturn(Optional.of(p));
        LocalDate fecha = LocalDate.of(2026, 3, 15);

        anticipoService.registrarAnticipo(PID, new BigDecimal("50.00"), fecha, UID);

        verify(presupuestoRepository).save(argThat(saved ->
                Boolean.TRUE.equals(saved.getTieneAnticipo())
                        && saved.getImporteAnticipo().compareTo(new BigDecimal("50.00")) == 0
                        && fecha.equals(saved.getFechaAnticipo())));
    }

    @Test
    void calcularResumenAnticipo_sinAnticipoRegistrado_flagsYceroEnAnticipo() {
        Presupuesto p = presupuestoAceptadoConLineaIva();
        p.setTieneAnticipo(false);
        when(presupuestoRepository.findByIdAndUsuarioId(PID, UID)).thenReturn(Optional.of(p));

        AnticipoResumenDTO r = anticipoService.calcularResumenAnticipo(PID, UID);

        assertThat(r.tieneAnticipoRegistrado()).isFalse();
        assertThat(r.anticipoYaFacturado()).isFalse();
        assertThat(r.importeAnticipo()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.totalPresupuesto()).isEqualByComparingTo(new BigDecimal("121.00"));
    }

    @Test
    void calcularResumenAnticipo_conAnticipoNoFacturado_baseIvaYpendienteCoherentes() {
        Presupuesto p = presupuestoAceptadoConLineaIva();
        p.setTieneAnticipo(true);
        p.setImporteAnticipo(new BigDecimal("100.00"));
        p.setFechaAnticipo(LocalDate.of(2026, 1, 10));
        p.setAnticipoFacturado(false);
        when(presupuestoRepository.findByIdAndUsuarioId(PID, UID)).thenReturn(Optional.of(p));

        AnticipoResumenDTO r = anticipoService.calcularResumenAnticipo(PID, UID);

        assertThat(r.tieneAnticipoRegistrado()).isTrue();
        assertThat(r.baseAnticipo()).isEqualByComparingTo(new BigDecimal("82.64"));
        assertThat(r.ivaAnticipo()).isEqualByComparingTo(new BigDecimal("17.36"));
        assertThat(r.basePendiente()).isEqualByComparingTo(new BigDecimal("17.36"));
        assertThat(r.ivaPendiente()).isEqualByComparingTo(new BigDecimal("3.64"));
        assertThat(r.importePendiente()).isEqualByComparingTo(new BigDecimal("21.00"));
    }

    @Test
    void generarFacturaAnticipo_yaEmitida_lanza400() {
        Presupuesto p = presupuestoAceptadoConLineaIva();
        p.setTieneAnticipo(true);
        p.setImporteAnticipo(new BigDecimal("100.00"));
        p.setFechaAnticipo(LocalDate.now());
        p.setAnticipoFacturado(true);
        when(presupuestoRepository.findByIdAndUsuarioId(PID, UID)).thenReturn(Optional.of(p));
        Usuario u = new Usuario();
        u.setId(UID);

        assertThatThrownBy(() -> anticipoService.generarFacturaAnticipo(PID, UID, u))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value()));
    }

    private static Presupuesto presupuestoAceptadoConLineaIva() {
        Presupuesto p = new Presupuesto();
        p.setId(PID);
        p.setEstado("Aceptado");
        p.setSubtotal(100.0);
        p.setIva(21.0);
        p.setTotal(121.0);
        p.setIvaHabilitado(true);
        Cliente c = new Cliente();
        c.setEstadoCliente(EstadoCliente.COMPLETO);
        p.setCliente(c);
        PresupuestoItem it = new PresupuestoItem();
        it.setAplicaIva(true);
        it.setSubtotal(100.0);
        it.setEsTareaManual(true);
        it.setTareaManual("Trabajo test");
        p.getItems().add(it);
        return p;
    }
}
