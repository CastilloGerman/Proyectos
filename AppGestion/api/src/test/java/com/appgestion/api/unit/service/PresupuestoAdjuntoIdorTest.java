package com.appgestion.api.unit.service;

import com.appgestion.api.domain.entity.Presupuesto;
import com.appgestion.api.repository.ClienteRepository;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.repository.FacturaRepository;
import com.appgestion.api.repository.MaterialRepository;
import com.appgestion.api.repository.PresupuestoRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.service.EmailService;
import com.appgestion.api.service.PresupuestoCondicionesService;
import com.appgestion.api.service.PresupuestoPdfService;
import com.appgestion.api.service.PresupuestoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Contrato anti-IDOR: foto y firma de presupuesto se filtran siempre por {@code usuarioId}.
 */
@ExtendWith(MockitoExtension.class)
class PresupuestoAdjuntoIdorTest {

    @Mock PresupuestoRepository presupuestoRepository;

    private PresupuestoService service() {
        return new PresupuestoService(
                presupuestoRepository,
                mock(ClienteRepository.class),
                mock(EmpresaRepository.class),
                mock(MaterialRepository.class),
                mock(FacturaRepository.class),
                mock(PresupuestoPdfService.class),
                mock(EmailService.class),
                mock(PresupuestoCondicionesService.class),
                mock(UsuarioRepository.class)
        );
    }

    @Test
    void guardarFoto_otroUsuario_devuelve404() {
        when(presupuestoRepository.findByIdAndUsuarioId(eq(99L), eq(2L))).thenReturn(Optional.empty());
        MockMultipartFile file = pngFile(32);

        assertThatThrownBy(() -> service().guardarFoto(99L, 2L, file))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(presupuestoRepository).findByIdAndUsuarioId(99L, 2L);
        verify(presupuestoRepository, never()).save(any());
    }

    @Test
    void obtenerFirma_otroUsuario_devuelve404() {
        when(presupuestoRepository.findByIdAndUsuarioId(eq(10L), eq(7L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().obtenerFirma(10L, 7L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(presupuestoRepository).findByIdAndUsuarioId(10L, 7L);
    }

    @Test
    void guardarFirma_otroUsuario_devuelve404() {
        when(presupuestoRepository.findByIdAndUsuarioId(eq(3L), eq(8L))).thenReturn(Optional.empty());
        MockMultipartFile file = pngFile(16);

        assertThatThrownBy(() -> service().guardarFirma(3L, 8L, file))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(presupuestoRepository, never()).save(any());
    }

    @Test
    void obtenerFoto_otroUsuario_devuelve404() {
        when(presupuestoRepository.findByIdAndUsuarioId(eq(4L), eq(9L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().obtenerFoto(4L, 9L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void guardarFoto_supera4Mb_rechaza() {
        Presupuesto presupuesto = new Presupuesto();
        when(presupuestoRepository.findByIdAndUsuarioId(1L, 1L)).thenReturn(Optional.of(presupuesto));
        byte[] huge = new byte[(int) PresupuestoService.MAX_ADJUNTO_BYTES + 1];
        MockMultipartFile file = new MockMultipartFile("file", "foto.jpg", "image/jpeg", huge);

        assertThatThrownBy(() -> service().guardarFoto(1L, 1L, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("4 MB");

        verify(presupuestoRepository, never()).save(any());
    }

    @Test
    void guardarFoto_propietario_persiste() throws IOException {
        Presupuesto presupuesto = new Presupuesto();
        when(presupuestoRepository.findByIdAndUsuarioId(1L, 1L)).thenReturn(Optional.of(presupuesto));
        when(presupuestoRepository.save(presupuesto)).thenReturn(presupuesto);
        MockMultipartFile file = pngFile(64);

        service().guardarFoto(1L, 1L, file);

        assertThat(presupuesto.getFotoTrabajo()).hasSize(64);
        verify(presupuestoRepository).save(presupuesto);
    }

    private static MockMultipartFile pngFile(int size) {
        byte[] data = new byte[size];
        data[0] = (byte) 0x89;
        data[1] = 0x50;
        data[2] = 0x4E;
        data[3] = 0x47;
        return new MockMultipartFile("file", "firma.png", "image/png", data);
    }
}
