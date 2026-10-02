package com.appgestion.api.unit.controller;

import com.appgestion.api.controller.PresupuestoController;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.CanalEnvio;
import com.appgestion.api.dto.request.MarcarPresupuestoEnviadoRequest;
import com.appgestion.api.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.*;

class PresupuestoEnvioControllerTest {
    @Test
    void marcarEnviado_usaUsuarioAutenticadoYDelegacionDeServicio() {
        CurrentUserService currentUser = mock(CurrentUserService.class);
        PresupuestoService presupuestos = mock(PresupuestoService.class);
        Usuario usuario = new Usuario();
        usuario.setId(18L);
        when(currentUser.getCurrentUsuario()).thenReturn(usuario);
        PresupuestoController controller = new PresupuestoController(presupuestos, mock(FacturaService.class),
                mock(AnticipoService.class), currentUser, mock(PresupuestoCondicionesService.class));

        controller.marcarEnviado(4L, new MarcarPresupuestoEnviadoRequest(CanalEnvio.EMAIL));

        verify(presupuestos).marcarEnviado(4L, 18L, CanalEnvio.EMAIL);
    }
}
