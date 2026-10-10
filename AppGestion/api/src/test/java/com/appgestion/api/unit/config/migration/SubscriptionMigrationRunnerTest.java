package com.appgestion.api.unit.config.migration;

import com.appgestion.api.config.migration.SubscriptionMigrationRunner;
import com.appgestion.api.domain.entity.AppMigrationState;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.repository.AppMigrationStateRepository;
import com.appgestion.api.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionMigrationRunnerTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AppMigrationStateRepository appMigrationStateRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private SubscriptionMigrationRunner runner;

    @Test
    @DisplayName("Debería saltar la migración si ya existe el flag en app_migration_state")
    void shouldSkipWhenMigrationAlreadyExecuted() {
        // Dado: la migración ya se ejecutó anteriormente
        AppMigrationState existing = new AppMigrationState("subscription_migration_v1", "5");
        when(appMigrationStateRepository.findByKey("subscription_migration_v1"))
                .thenReturn(Optional.of(existing));

        // Cuando: se invoca la migración
        runner.migrateExistingUsers();

        // Entonces: NO se deben cargar los usuarios ni guardar nada nuevo
        verify(usuarioRepository, never()).findAll();
        verify(appMigrationStateRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debería ejecutar la migración y guardar el flag si no existe")
    void shouldRunAndSaveFlagWhenNotExecuted() {
        // Dado: la migración no se ha ejecutado
        when(appMigrationStateRepository.findByKey("subscription_migration_v1"))
                .thenReturn(Optional.empty());

        Usuario user = new Usuario();
        when(usuarioRepository.findAll()).thenReturn(List.of(user));

        // Cuando: se invoca la migración
        runner.migrateExistingUsers();

        // Entonces: se cargaron los usuarios y se guardó el flag
        verify(usuarioRepository).findAll();
        verify(appMigrationStateRepository).save(argThat(state ->
                "subscription_migration_v1".equals(state.getKey())
        ));
    }

    @Test
    @DisplayName("Debería no cargar usuarios en el segundo arranque simulado")
    void shouldNotLoadUsersOnSecondStartup() {
        // Primer arranque: no existe el flag
        when(appMigrationStateRepository.findByKey("subscription_migration_v1"))
                .thenReturn(Optional.empty());
        when(usuarioRepository.findAll()).thenReturn(List.of());

        runner.migrateExistingUsers();

        verify(appMigrationStateRepository).save(any());

        // Segundo arranque: ahora sí existe el flag
        AppMigrationState existing = new AppMigrationState("subscription_migration_v1", "0");
        when(appMigrationStateRepository.findByKey("subscription_migration_v1"))
                .thenReturn(Optional.of(existing));

        runner.migrateExistingUsers();

        // findAll se llamó solo una vez (primer arranque), no en el segundo
        verify(usuarioRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Debería fallar con mensaje claro si falta la tabla app_migration_state")
    void shouldFailWithClearMessageWhenTableMissing() {
        // Dado: la consulta falla porque la tabla no existe
        when(appMigrationStateRepository.findByKey("subscription_migration_v1"))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("table app_migration_state does not exist"));

        // Cuando/Entonces: lanza IllegalStateException con mensaje accionable
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> runner.migrateExistingUsers());

        // El mensaje debe mencionar la tabla y el procedimiento
        String msg = ex.getMessage();
        assert msg.contains("app_migration_state");
        assert msg.contains("Flyway") : "El mensaje debe mencionar Flyway: " + msg;
        assert msg.contains("prod") : "El mensaje debe mencionar el perfil prod: " + msg;
    }
}
