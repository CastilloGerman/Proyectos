package com.appgestion.api.unit.service;

import com.appgestion.api.domain.entity.Cliente;
import com.appgestion.api.domain.entity.Factura;
import com.appgestion.api.domain.entity.FacturaItem;
import com.appgestion.api.dto.response.FacturaResponse;
import com.appgestion.api.repository.ClienteRepository;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.repository.FacturaCobroRepository;
import com.appgestion.api.repository.FacturaRepository;
import com.appgestion.api.repository.MaterialRepository;
import com.appgestion.api.repository.PresupuestoRepository;
import com.appgestion.api.service.ClienteService;
import com.appgestion.api.service.FacturaAuditoriaService;
import com.appgestion.api.service.FacturaCobroService;
import com.appgestion.api.service.FacturaEmailService;
import com.appgestion.api.service.FacturaNumeroService;
import com.appgestion.api.service.FacturaPaymentLinkService;
import com.appgestion.api.service.FacturaPdfService;
import com.appgestion.api.service.FacturaResponseMapper;
import com.appgestion.api.service.FacturaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacturaServiceListarTest {

    @Mock FacturaRepository facturaRepository;

    FacturaService service;

    @BeforeEach
    void setUp() {
        service = new FacturaService(
                facturaRepository,
                org.mockito.Mockito.mock(ClienteRepository.class),
                org.mockito.Mockito.mock(EmpresaRepository.class),
                org.mockito.Mockito.mock(PresupuestoRepository.class),
                org.mockito.Mockito.mock(MaterialRepository.class),
                org.mockito.Mockito.mock(FacturaPdfService.class),
                org.mockito.Mockito.mock(FacturaNumeroService.class),
                org.mockito.Mockito.mock(FacturaCobroRepository.class),
                new FacturaResponseMapper(),
                org.mockito.Mockito.mock(FacturaEmailService.class),
                org.mockito.Mockito.mock(FacturaCobroService.class),
                org.mockito.Mockito.mock(FacturaPaymentLinkService.class),
                org.mockito.Mockito.mock(ClienteService.class),
                org.mockito.Mockito.mock(FacturaAuditoriaService.class));
    }

    @Test
    void listar_vacio_noConsultaElGrafo() {
        when(facturaRepository.findIdsByUsuarioIdForList(7L, false)).thenReturn(List.of());

        assertThat(service.listar(7L, null, false)).isEmpty();

        verify(facturaRepository, never()).findForListByIds(any());
    }

    @Test
    void listar_noDevuelvePartidas() {
        Cliente cliente = new Cliente();
        cliente.setId(3L);
        cliente.setNombre("Ana");
        Factura factura = new Factura();
        factura.setId(11L);
        factura.setCliente(cliente);
        factura.setNumeroFactura("F-11");
        factura.setSubtotal(20.0);
        factura.setIva(4.2);
        factura.setTotal(24.2);
        FacturaItem partida = new FacturaItem();
        partida.setId(5L);
        partida.setEsTareaManual(true);
        partida.setTareaManual("Pintura");
        partida.setCantidad(2.0);
        partida.setPrecioUnitario(10.0);
        partida.setSubtotal(20.0);
        factura.setItems(new ArrayList<>(List.of(partida)));

        when(facturaRepository.findIdsByUsuarioIdForList(7L, false)).thenReturn(List.of(11L));
        when(facturaRepository.findForListByIds(List.of(11L))).thenReturn(List.of(factura));

        List<FacturaResponse> result = service.listar(7L, null, false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).items()).isEmpty();
        assertThat(result.get(0).numeroFactura()).isEqualTo("F-11");
        assertThat(result.get(0).clienteNombre()).isEqualTo("Ana");
        assertThat(result.get(0).total()).isEqualTo(24.2);
    }
}
