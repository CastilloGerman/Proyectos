package com.appgestion.api.unit.service;

import com.appgestion.api.domain.entity.Cliente;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.EstadoCliente;
import com.appgestion.api.repository.ClienteRepository;
import com.appgestion.api.service.ClienteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import org.assertj.core.api.Assertions;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    ClienteRepository clienteRepository;

    @InjectMocks
    ClienteService clienteService;

    @Test
    void validarClienteParaFactura_provisional_lanza400() {
        Cliente c = clienteConEstado(EstadoCliente.PROVISIONAL);
        when(clienteRepository.findByIdAndUsuarioId(eq(10L), eq(1L))).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> clienteService.validarClienteParaFactura(10L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException r = (ResponseStatusException) ex;
                    Assertions.assertThat(r.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());
                    Assertions.assertThat(r.getReason()).isEqualTo(ClienteService.MSG_FACTURA_CLIENTE_INCOMPLETO);
                });
    }

    @Test
    void validarClienteParaFactura_completo_noLanza() {
        Cliente c = clienteConEstado(EstadoCliente.COMPLETO);
        when(clienteRepository.findByIdAndUsuarioId(eq(10L), eq(1L))).thenReturn(Optional.of(c));

        assertThatCode(() -> clienteService.validarClienteParaFactura(10L, 1L)).doesNotThrowAnyException();
    }

    private static Cliente clienteConEstado(EstadoCliente estado) {
        Usuario u = new Usuario();
        u.setId(1L);
        Cliente c = new Cliente();
        c.setId(10L);
        c.setUsuario(u);
        c.setNombre("Test");
        c.setEstadoCliente(estado);
        return c;
    }
}
