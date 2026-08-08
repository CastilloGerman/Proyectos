package com.appgestion.api.unit.service;

import com.appgestion.api.domain.entity.Gasto;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.GastoCategoria;
import com.appgestion.api.dto.request.GastoRequest;
import com.appgestion.api.repository.GastoRepository;
import com.appgestion.api.service.GastoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Contrato anti-IDOR: el acceso a gastos debe filtrarse siempre por {@code usuarioId}.
 */
@ExtendWith(MockitoExtension.class)
class GastoServiceIdorTest {

    @Mock
    GastoRepository gastoRepository;

    GastoService gastoService;

    @BeforeEach
    void setUp() {
        gastoService = new GastoService(gastoRepository);
    }

    @Test
    void obtenerPorId_otroUsuario_devuelve404() {
        when(gastoRepository.findByIdAndUsuarioId(eq(99L), eq(2L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gastoService.obtenerPorId(99L, 2L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(gastoRepository).findByIdAndUsuarioId(99L, 2L);
    }

    @Test
    void actualizar_otroUsuario_devuelve404() {
        GastoRequest request = sampleRequest();
        when(gastoRepository.findByIdAndUsuarioId(10L, 5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gastoService.actualizar(10L, request, 5L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(gastoRepository).findByIdAndUsuarioId(10L, 5L);
        verify(gastoRepository, never()).save(any());
    }

    @Test
    void eliminar_otroUsuario_devuelve404() {
        when(gastoRepository.existsByIdAndUsuarioId(3L, 7L)).thenReturn(false);

        assertThatThrownBy(() -> gastoService.eliminar(3L, 7L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(gastoRepository).existsByIdAndUsuarioId(3L, 7L);
        verify(gastoRepository, never()).deleteById(any());
    }

    @Test
    void listar_consultaSoloGastosDelUsuario() {
        when(gastoRepository.findByUsuarioIdOrderByFechaDesc(42L)).thenReturn(List.of());

        gastoService.listar(42L);

        verify(gastoRepository).findByUsuarioIdOrderByFechaDesc(42L);
    }

    @Test
    void crear_asignaUsuarioDelTenant() {
        Usuario usuario = new Usuario();
        usuario.setId(5L);
        GastoRequest request = sampleRequest();
        when(gastoRepository.save(any(Gasto.class))).thenAnswer(inv -> {
            Gasto g = inv.getArgument(0);
            g.setId(1L);
            return g;
        });

        gastoService.crear(request, usuario);

        verify(gastoRepository).save(any(Gasto.class));
    }

    private static GastoRequest sampleRequest() {
        return new GastoRequest("Proveedor", "Concepto", LocalDate.now(), 100.0, 21.0, GastoCategoria.OTROS);
    }
}
