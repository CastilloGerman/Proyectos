package com.appgestion.api.unit.service;

import com.appgestion.api.config.CacheConfig;
import com.appgestion.api.config.CacheNames;
import com.appgestion.api.domain.entity.Material;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.dto.request.MaterialRequest;
import com.appgestion.api.repository.MaterialRepository;
import com.appgestion.api.service.MaterialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Aislamiento multi-tenant de la caché {@link CacheNames#MATERIALES_TOP_USADOS}:
 * la clave debe ser {@code usuarioId}; dos usuarios nunca comparten entrada.
 */
@SpringBootTest(classes = {CacheConfig.class, MaterialService.class})
class MaterialServiceTopUsadosCacheTest {

    @MockitoBean
    MaterialRepository materialRepository;

    @Autowired
    MaterialService materialService;

    @Autowired
    CacheManager cacheManager;

    @BeforeEach
    void clearCache() {
        var cache = cacheManager.getCache(CacheNames.MATERIALES_TOP_USADOS);
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void findTop5MasUsados_dosUsuarios_noCompartenEntradaDeCache() {
        when(materialRepository.findTop5MasUsadosByUsuarioId(1L))
                .thenReturn(List.of(material(10L, "Material usuario 1")));
        when(materialRepository.findTop5MasUsadosByUsuarioId(2L))
                .thenReturn(List.of(material(20L, "Material usuario 2")));

        assertThat(materialService.findTop5MasUsados(1L).getFirst().nombre()).isEqualTo("Material usuario 1");
        assertThat(materialService.findTop5MasUsados(2L).getFirst().nombre()).isEqualTo("Material usuario 2");

        when(materialRepository.findTop5MasUsadosByUsuarioId(1L))
                .thenReturn(List.of(material(10L, "CAMBIO NO DEBE VERSE")));
        when(materialRepository.findTop5MasUsadosByUsuarioId(2L))
                .thenReturn(List.of(material(20L, "CAMBIO NO DEBE VERSE")));

        assertThat(materialService.findTop5MasUsados(1L).getFirst().nombre()).isEqualTo("Material usuario 1");
        assertThat(materialService.findTop5MasUsados(2L).getFirst().nombre()).isEqualTo("Material usuario 2");

        verify(materialRepository, times(1)).findTop5MasUsadosByUsuarioId(1L);
        verify(materialRepository, times(1)).findTop5MasUsadosByUsuarioId(2L);
    }

    @Test
    void crear_invalidaCacheTopUsadosDelUsuario() {
        when(materialRepository.findTop5MasUsadosByUsuarioId(1L))
                .thenReturn(List.of(material(10L, "Ranking inicial")))
                .thenReturn(List.of(material(10L, "Ranking inicial"), material(11L, "Nuevo")));

        assertThat(materialService.findTop5MasUsados(1L)).hasSize(1);

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        Material saved = material(11L, "Nuevo");
        saved.setUsuario(usuario);
        when(materialRepository.save(any(Material.class))).thenReturn(saved);

        materialService.crear(new MaterialRequest("Nuevo", 12.5, "ud"), usuario);

        assertThat(materialService.findTop5MasUsados(1L)).hasSize(2);
        verify(materialRepository, times(2)).findTop5MasUsadosByUsuarioId(1L);
    }

    @Test
    void actualizar_invalidaCacheTopUsadosDelUsuario() {
        when(materialRepository.findTop5MasUsadosByUsuarioId(1L))
                .thenReturn(List.of(material(10L, "Antes")))
                .thenReturn(List.of(material(10L, "Después")));

        materialService.findTop5MasUsados(1L);

        Material existing = material(10L, "Antes");
        when(materialRepository.findByIdAndUsuarioId(10L, 1L)).thenReturn(Optional.of(existing));
        when(materialRepository.save(any(Material.class))).thenAnswer(inv -> inv.getArgument(0));

        materialService.actualizar(10L, new MaterialRequest("Después", 5.0, "ud"), 1L);

        assertThat(materialService.findTop5MasUsados(1L).getFirst().nombre()).isEqualTo("Después");
        verify(materialRepository, times(2)).findTop5MasUsadosByUsuarioId(1L);
    }

    @Test
    void eliminar_invalidaCacheTopUsadosDelUsuario() {
        when(materialRepository.findTop5MasUsadosByUsuarioId(1L))
                .thenReturn(List.of(material(10L, "Solo uno")))
                .thenReturn(List.of());

        materialService.findTop5MasUsados(1L);

        when(materialRepository.existsByIdAndUsuarioId(10L, 1L)).thenReturn(true);

        materialService.eliminar(10L, 1L);

        assertThat(materialService.findTop5MasUsados(1L)).isEmpty();
        verify(materialRepository, times(2)).findTop5MasUsadosByUsuarioId(1L);
    }

    private static Material material(long id, String nombre) {
        Material m = new Material();
        m.setId(id);
        m.setNombre(nombre);
        m.setPrecioUnitario(1.0);
        m.setUnidadMedida("ud");
        return m;
    }
}
