package com.appgestion.api.repository;

import com.appgestion.api.domain.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    List<Material> findByUsuarioId(Long usuarioId);

    Optional<Material> findByIdAndUsuarioId(Long id, Long usuarioId);

    @Query("SELECT m FROM Material m WHERE m.usuario.id = :usuarioId " +
            "AND LOWER(TRIM(m.nombre)) = LOWER(TRIM(:nombre))")
    Optional<Material> findByUsuarioIdAndNombreNormalizado(
            @Param("usuarioId") Long usuarioId,
            @Param("nombre") String nombre);

    boolean existsByIdAndUsuarioId(Long id, Long usuarioId);

    @Query(value = "SELECT m.* FROM materiales m " +
            "INNER JOIN presupuesto_items pi ON pi.material_id = m.id " +
            "INNER JOIN presupuestos p ON p.id = pi.presupuesto_id " +
            "WHERE p.usuario_id = ?1 " +
            "GROUP BY m.id " +
            "ORDER BY COUNT(pi.id) DESC " +
            "LIMIT 5", nativeQuery = true)
    List<Material> findTop5MasUsadosByUsuarioId(Long usuarioId);
}
