package com.appgestion.api.unit.scheduler;

import com.appgestion.api.controller.LocalPresupuestoSeguimientoController;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.scheduler.PresupuestoSeguimientoJob;
import com.appgestion.api.service.PresupuestoSeguimientoService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.context.annotation.Profile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PresupuestoSeguimientoJobTest {
    @Test
    void continuesAfterOneOwnersFailure() {
        UsuarioRepository users = mock(UsuarioRepository.class);
        PresupuestoSeguimientoService service = mock(PresupuestoSeguimientoService.class);
        when(users.findFollowupOwnerIdsAfter(0L, PageRequest.of(0, 100)))
                .thenReturn(new PageImpl<>(List.of(10L, 11L)));
        when(service.processOwner(10L, null)).thenThrow(new IllegalStateException("unimportant"));
        when(service.processOwner(11L, null)).thenReturn(2);

        PresupuestoSeguimientoJob job = new PresupuestoSeguimientoJob(users, service);

        job.ejecutarDiario();

        verify(service).processOwner(10L, null);
        verify(service).processOwner(11L, null);
    }

    @Test
    void manualEndpointIsRegisteredOnlyForLocalProfile() {
        Profile profile = LocalPresupuestoSeguimientoController.class.getAnnotation(Profile.class);

        assertThat(profile).isNotNull();
        assertThat(profile.value()).containsExactly("local");
    }
}
