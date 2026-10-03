package com.appgestion.api.unit.service;

import com.appgestion.api.domain.entity.Material;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.dto.request.MaterialRequest;
import com.appgestion.api.repository.MaterialRepository;
import com.appgestion.api.service.MaterialService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialServiceDuplicadoTest {

    @Mock
    MaterialRepository materialRepository;

    @InjectMocks
    MaterialService materialService;

    @Test
    void crear_reutilizaNombreExistenteDelMismoUsuarioSinInsertarDuplicado() {
        Usuario usuario = new Usuario();
        usuario.setId(41L);
        Material existente = new Material();
        existente.setId(123L);
        existente.setUsuario(usuario);
        existente.setNombre("Retirada de escombros");
        existente.setPrecioUnitario(35.0);
        existente.setUnidadMedida("viaje");
        when(materialRepository.findByUsuarioIdAndNombreNormalizado(41L, "  retirada DE escombros "))
                .thenReturn(Optional.of(existente));

        var respuesta = materialService.crear(
                new MaterialRequest("  retirada DE escombros ", 0.0, "ud"), usuario);

        assertThat(respuesta.id()).isEqualTo(123L);
        assertThat(respuesta.nombre()).isEqualTo("Retirada de escombros");
        assertThat(respuesta.precioUnitario()).isEqualTo(35.0);
        verify(materialRepository).findByUsuarioIdAndNombreNormalizado(41L, "  retirada DE escombros ");
        verify(materialRepository, never()).save(org.mockito.ArgumentMatchers.any(Material.class));
    }
}
