package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Factura;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Contrato anti-IDOR: el acceso a facturas debe filtrarse siempre por {@code usuarioId}.
 */
@ExtendWith(MockitoExtension.class)
class FacturaServiceIdorTest {

    @Mock FacturaRepository facturaRepository;
    @Mock FacturaPdfService facturaPdfService;
    @Mock FacturaResponseMapper facturaResponseMapper;

    FacturaService facturaService;

    @BeforeEach
    void setUp() {
        facturaService = new FacturaService(
                facturaRepository,
                mock(ClienteRepository.class),
                mock(EmpresaRepository.class),
                mock(PresupuestoRepository.class),
                mock(MaterialRepository.class),
                facturaPdfService,
                mock(FacturaNumeroService.class),
                mock(FacturaCobroRepository.class),
                facturaResponseMapper,
                mock(FacturaEmailService.class),
                mock(FacturaCobroService.class),
                mock(FacturaPaymentLinkService.class),
                mock(ClienteService.class));
    }

    @Test
    void obtenerPorId_otroUsuario_devuelve404() {
        when(facturaRepository.findByIdAndUsuarioId(eq(99L), eq(2L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facturaService.obtenerPorId(99L, 2L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(facturaRepository).findByIdAndUsuarioId(99L, 2L);
        verifyNoInteractions(facturaResponseMapper);
    }

    @Test
    void generarPdf_pasaUsuarioIdAlRepositorio() {
        Factura factura = new Factura();
        Usuario u = new Usuario();
        u.setId(5L);
        factura.setUsuario(u);
        when(facturaRepository.findByIdAndUsuarioId(10L, 5L)).thenReturn(Optional.of(factura));
        when(facturaPdfService.generarPdf(factura, 5L)).thenReturn(new byte[]{1, 2});

        facturaService.generarPdf(10L, 5L);

        verify(facturaRepository).findByIdAndUsuarioId(10L, 5L);
        verify(facturaPdfService).generarPdf(factura, 5L);
    }

    @Test
    void anular_facturaInexistente_devuelve404() {
        when(facturaRepository.findByIdAndUsuarioId(3L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facturaService.anular(3L, 7L, null))
                .isInstanceOf(ResponseStatusException.class);

        verify(facturaRepository).findByIdAndUsuarioId(3L, 7L);
        verify(facturaRepository, never()).save(any());
    }

    @Test
    void listar_consultaSoloFacturasDelUsuario() {
        when(facturaRepository.findByUsuarioIdForList(42L, false)).thenReturn(List.of());

        facturaService.listar(42L, null, false);

        verify(facturaRepository).findByUsuarioIdForList(42L, false);
    }
}
