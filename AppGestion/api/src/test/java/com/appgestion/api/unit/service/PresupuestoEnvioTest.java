package com.appgestion.api.unit.service;

import com.appgestion.api.domain.entity.Presupuesto;
import com.appgestion.api.domain.entity.Cliente;
import com.appgestion.api.domain.enums.CanalEnvio;
import com.appgestion.api.dto.request.EnviarEmailRequest;
import jakarta.validation.Validation;
import com.appgestion.api.repository.*;
import com.appgestion.api.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class PresupuestoEnvioTest {
    @Mock PresupuestoRepository presupuestoRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock EmpresaRepository empresaRepository;
    @Mock MaterialRepository materialRepository;
    @Mock FacturaRepository facturaRepository;
    @Mock PresupuestoPdfService pdfService;
    @Mock EmailService emailService;
    @Mock PresupuestoCondicionesService condicionesService;
    @Mock UsuarioRepository usuarioRepository;

    private PresupuestoService service() {
        return new PresupuestoService(presupuestoRepository, clienteRepository, empresaRepository, materialRepository,
                facturaRepository, pdfService, emailService, condicionesService, usuarioRepository);
    }

    @Test
    void marcarEnviado_filtraPorUsuarioYConservaEstadoComercial() {
        Presupuesto p = new Presupuesto();
        p.setEstado("Aceptado");
        when(presupuestoRepository.findByIdAndUsuarioId(5L, 22L)).thenReturn(Optional.of(p));

        service().marcarEnviado(5L, 22L, CanalEnvio.WHATSAPP);

        assertThat(p.getEstado()).isEqualTo("Aceptado");
        assertThat(p.getCanalEnvio()).isEqualTo("WHATSAPP");
        assertThat(p.getEnviadoAt()).isNotNull();
        verify(presupuestoRepository).save(p);
        verify(presupuestoRepository, never()).findById(5L);
    }

    @Test
    void marcarEnviado_presupuestoAjenoResponde404SinGuardar() {
        when(presupuestoRepository.findByIdAndUsuarioId(5L, 22L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().marcarEnviado(5L, 22L, CanalEnvio.WHATSAPP))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(presupuestoRepository, never()).save(any());
    }

    @Test
    void enviarEmail_escapaMensajeYEncolaAdjuntoAntesDeRegistrarEnvio() {
        Presupuesto p = new Presupuesto();
        p.setId(5L);
        Cliente cliente = new Cliente();
        cliente.setNombre("<script>Cliente</script>");
        cliente.setEmail("cliente@example.test");
        p.setCliente(cliente);
        when(presupuestoRepository.findByIdAndUsuarioId(5L, 22L)).thenReturn(Optional.of(p));
        when(pdfService.generarPdf(p, 22L)).thenReturn(new byte[]{1, 2});
        when(empresaRepository.findByUsuarioId(22L)).thenReturn(Optional.empty());

        service().enviarPorEmail(5L, 22L, new EnviarEmailRequest("destino@example.test", "Asunto seguro", "<script>& Hola"));

        org.mockito.ArgumentCaptor<String> body = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarPdf(eq(22L), eq("destino@example.test"), eq("Asunto seguro"), body.capture(),
                eq(new byte[]{1, 2}), anyString());
        assertThat(body.getValue())
                .contains("&lt;script&gt;&amp; Hola", "&lt;script&gt;Cliente&lt;/script&gt;")
                .doesNotContain("<script>");
        assertThat(p.getCanalEnvio()).isEqualTo("EMAIL");
    }

    @Test
    void enviarEmail_asuntoConSaltoDeLineaSeRechaza() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(
                    new EnviarEmailRequest("destino@example.test", "Asunto\r\nBcc: atacante@example.test", "Mensaje"));

            assertThat(violations).anySatisfy(v -> assertThat(v.getPropertyPath().toString()).isEqualTo("asunto"));
        }
    }

    @Test
    void enviarEmail_soloEmailMantieneCompatibilidadYAdjuntaPdf() throws Exception {
        Presupuesto p = new Presupuesto();
        p.setId(5L);
        Cliente cliente = new Cliente();
        cliente.setNombre("Cliente");
        cliente.setEmail("cliente@example.test");
        p.setCliente(cliente);
        when(presupuestoRepository.findByIdAndUsuarioId(5L, 22L)).thenReturn(Optional.of(p));
        when(pdfService.generarPdf(p, 22L)).thenReturn(new byte[]{3, 4});
        when(empresaRepository.findByUsuarioId(22L)).thenReturn(Optional.empty());

        EnviarEmailRequest request = new ObjectMapper().readValue(
                "{\"email\":\"destino@example.test\"}", EnviarEmailRequest.class);
        service().enviarPorEmail(5L, 22L, request);

        org.mockito.ArgumentCaptor<String> body = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarPdf(eq(22L), eq("destino@example.test"), eq("Presupuesto - Cliente"), body.capture(),
                eq(new byte[]{3, 4}), anyString());
        assertThat(body.getValue()).contains("Adjunto");
        assertThat(p.getCanalEnvio()).isEqualTo("EMAIL");
    }
}
