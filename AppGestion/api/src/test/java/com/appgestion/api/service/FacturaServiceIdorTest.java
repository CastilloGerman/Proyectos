package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Factura;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
    @Mock ClienteRepository clienteRepository;
    @Mock EmpresaRepository empresaRepository;
    @Mock PresupuestoRepository presupuestoRepository;
    @Mock MaterialRepository materialRepository;
    @Mock FacturaPdfService facturaPdfService;
    @Mock FacturaNumeroService facturaNumeroService;
    @Mock FacturaCobroRepository facturaCobroRepository;
    @Mock FacturaResponseMapper facturaResponseMapper;
    @Mock FacturaEmailService facturaEmailService;
    @Mock FacturaCobroService facturaCobroService;
    @Mock FacturaPaymentLinkService facturaPaymentLinkService;

    @InjectMocks FacturaService facturaService;

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
    void eliminar_verificaExistenciaPorUsuario() {
        when(facturaRepository.existsByIdAndUsuarioId(3L, 7L)).thenReturn(false);

        assertThatThrownBy(() -> facturaService.eliminar(3L, 7L))
                .isInstanceOf(ResponseStatusException.class);

        verify(facturaRepository).existsByIdAndUsuarioId(3L, 7L);
        verify(facturaRepository, never()).deleteById(anyLong());
    }

    @Test
    void listar_consultaSoloFacturasDelUsuario() {
        when(facturaRepository.findByUsuarioIdOrderByFechaCreacionDesc(42L)).thenReturn(List.of());

        facturaService.listar(42L, null);

        verify(facturaRepository).findByUsuarioIdOrderByFechaCreacionDesc(42L);
    }
}
