package com.appgestion.api.unit.service;

import com.appgestion.api.domain.entity.Notificacion;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.repository.NotificacionRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.service.NotificacionService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificacionPresupuestoSeguimientoTest {
    @Test
    void localizesFollowupNoticesInAllSupportedLanguages() {
        NotificacionRepository notifications = mock(NotificacionRepository.class);
        NotificacionService service = new NotificacionService(notifications, mock(UsuarioRepository.class));
        String[][] cases = {
                {"es-ES", "Presupuesto aún no visto", "Presupuesto visto, sin respuesta", "no ha respondido"},
                {"en-GB", "Estimate not viewed yet", "Estimate awaiting your follow-up", "has not replied"},
                {"fr-FR", "Devis pas encore consulté", "Devis consulté, sans réponse", "n’a pas répondu"},
                {"ro-RO", "Oferta nu a fost încă vizualizată", "Oferta vizualizată, fără răspuns", "nu a răspuns"},
                {"uk-UA", "Кошторис ще не переглянули", "Кошторис переглянуто, відповіді немає", "не відповів"}
        };

        for (String[] localeCase : cases) {
            Usuario owner = new Usuario();
            owner.setUiLocale(localeCase[0]);
            service.presupuestoSeguimiento(owner, "Cliente", 42L, "NO_ABIERTO", 3);
            assertLatestNotice(notifications, localeCase[1], "3");
            service.presupuestoSeguimiento(owner, "Cliente", 42L, "ABIERTO_SIN_RESPUESTA", 3);
            assertLatestNotice(notifications, localeCase[2], localeCase[3]);
        }
        verify(notifications, times(cases.length * 2)).save(any(Notificacion.class));
    }

    private static void assertLatestNotice(NotificacionRepository notifications, String title, String summaryText) {
        var captor = org.mockito.ArgumentCaptor.forClass(Notificacion.class);
        verify(notifications, atLeastOnce()).save(captor.capture());
        Notificacion notification = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(notification.getTitulo()).isEqualTo(title);
        assertThat(notification.getActionPath()).isEqualTo("/presupuestos/42");
        assertThat(notification.getResumen()).contains(summaryText);
    }
}
