package com.appgestion.api.unit.service;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GastoServiceValidationTest {

    @Mock
    GastoRepository gastoRepository;

    GastoService gastoService;

    @BeforeEach
    void setUp() {
        gastoService = new GastoService(gastoRepository);
    }

    @Test
    void crear_fechaFutura_devuelve400() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        GastoRequest request = new GastoRequest(
                "Proveedor",
                "Concepto",
                LocalDate.now().plusDays(1),
                100.0,
                21.0,
                GastoCategoria.OTROS);

        assertThatThrownBy(() -> gastoService.crear(request, usuario))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(gastoRepository, never()).save(any());
    }

    @Test
    void crear_baseImponibleNegativa_devuelve400() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        GastoRequest request = new GastoRequest(
                "Proveedor",
                "Concepto",
                LocalDate.now(),
                -1.0,
                21.0,
                GastoCategoria.OTROS);

        assertThatThrownBy(() -> gastoService.crear(request, usuario))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(gastoRepository, never()).save(any());
    }

    @Test
    void crear_proveedorVacio_devuelve400() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        GastoRequest request = new GastoRequest(
                "   ",
                "Concepto",
                LocalDate.now(),
                100.0,
                21.0,
                GastoCategoria.OTROS);

        assertThatThrownBy(() -> gastoService.crear(request, usuario))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(gastoRepository, never()).save(any());
    }
}
