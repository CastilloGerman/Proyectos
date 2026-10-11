package com.appgestion.api.unit.service;

import com.appgestion.api.domain.entity.Cliente;
import com.appgestion.api.domain.entity.Presupuesto;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.EstadoCliente;
import com.appgestion.api.domain.enums.TipoFactura;
import com.appgestion.api.dto.response.PresupuestoResponse;
import com.appgestion.api.repository.ClienteRepository;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.repository.FacturaRepository;
import com.appgestion.api.repository.MaterialRepository;
import com.appgestion.api.repository.PresupuestoEnlaceRepository;
import com.appgestion.api.repository.PresupuestoRepository;
import com.appgestion.api.repository.PresupuestoSeguimientoAvisoRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.service.EmailService;
import com.appgestion.api.service.PresupuestoCondicionesService;
import com.appgestion.api.service.PresupuestoPdfService;
import com.appgestion.api.service.PresupuestoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresupuestoServiceListarTest {

    @Mock PresupuestoRepository presupuestoRepository;
    @Mock FacturaRepository facturaRepository;
    @Mock PresupuestoEnlaceRepository enlaceRepository;
    @Mock PresupuestoSeguimientoAvisoRepository avisoRepository;
    @Mock PresupuestoCondicionesService condicionesService;

    PresupuestoService service;

    @BeforeEach
    void setUp() {
        service = new PresupuestoService(
                presupuestoRepository,
                org.mockito.Mockito.mock(ClienteRepository.class),
                org.mockito.Mockito.mock(EmpresaRepository.class),
                org.mockito.Mockito.mock(MaterialRepository.class),
                facturaRepository,
                org.mockito.Mockito.mock(PresupuestoPdfService.class),
                org.mockito.Mockito.mock(EmailService.class),
                condicionesService,
                org.mockito.Mockito.mock(UsuarioRepository.class),
                enlaceRepository,
                avisoRepository);
    }

    @Test
    void listar_vacio_noConsultaElGrafo() {
        when(presupuestoRepository.findIdsByUsuarioIdOrderByFechaCreacionDesc(7L)).thenReturn(List.of());

        assertThat(service.listar(7L, null)).isEmpty();

        verify(presupuestoRepository, never()).findForListByIds(any());
        verify(facturaRepository, never()).findVentaPrincipalIdPorPresupuestos(any(), any(), any(), any());
    }

    @Test
    void listar_resuelveFacturaEnlaceYAvisosEnBloque() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        Cliente cliente = new Cliente();
        cliente.setId(3L);
        cliente.setNombre("Ana");
        cliente.setEstadoCliente(EstadoCliente.COMPLETO);
        Presupuesto presupuesto = new Presupuesto();
        presupuesto.setId(11L);
        presupuesto.setUsuario(usuario);
        presupuesto.setCliente(cliente);
        presupuesto.setEstado("Pendiente");
        presupuesto.setItems(new ArrayList<>());

        Instant vista = Instant.parse("2026-01-01T00:00:00Z");
        Instant aviso = Instant.parse("2026-01-02T00:00:00Z");
        when(presupuestoRepository.findIdsByUsuarioIdOrderByFechaCreacionDesc(7L)).thenReturn(List.of(11L));
        when(presupuestoRepository.findForListByIds(List.of(11L))).thenReturn(List.of(presupuesto));
        when(facturaRepository.findVentaPrincipalIdPorPresupuestos(
                eq(7L), eq(List.of(11L)), eq(TipoFactura.NORMAL), eq(TipoFactura.FINAL_CON_ANTICIPO)))
                .thenReturn(List.<Object[]>of(new Object[]{11L, 99L}));
        when(enlaceRepository.summarizeViewsForPresupuestos(7L, List.of(11L)))
                .thenReturn(List.<Object[]>of(new Object[]{11L, vista, 2L}));
        when(avisoRepository.resumenPorPresupuestoIds(List.of(11L)))
                .thenReturn(List.<Object[]>of(new Object[]{11L, 1L, aviso}));
        when(condicionesService.desdeJson(null)).thenReturn(List.of());

        List<PresupuestoResponse> result = service.listar(7L, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).facturaId()).isEqualTo(99L);
        assertThat(result.get(0).enlacePrimeraVistaAt()).isEqualTo(vista);
        assertThat(result.get(0).enlaceNumVistas()).isEqualTo(2L);
        assertThat(result.get(0).seguimientoAvisosEnviados()).isEqualTo(1L);
        assertThat(result.get(0).seguimientoUltimoAvisoAt()).isEqualTo(aviso);
        verify(facturaRepository, never()).findVentasPrincipalesPorPresupuesto(any(), any(), any(), any());
        verify(enlaceRepository, never()).summarizeViews(any(), any());
        verify(avisoRepository, never()).countByPresupuestoId(any());
    }
}
