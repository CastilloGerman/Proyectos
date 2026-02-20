package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Material;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.dto.request.MaterialRequest;
import com.appgestion.api.dto.response.MaterialResponse;
import com.appgestion.api.repository.MaterialRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
public class MaterialService {

    private final MaterialRepository materialRepository;

    public MaterialService(MaterialRepository materialRepository) {
        this.materialRepository = materialRepository;
    }

    public List<MaterialResponse> listar(Long usuarioId) {
        return materialRepository.findByUsuarioId(usuarioId).stream()
                .map(this::toResponse)
                .toList();
    }

    public MaterialResponse obtenerPorId(Long id, Long usuarioId) {
        Material material = materialRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material no encontrado"));
        return toResponse(material);
    }

    @Transactional
    public MaterialResponse crear(MaterialRequest request, Usuario usuario) {
        Material material = new Material();
        material.setUsuario(usuario);
        mapRequestToEntity(request, material);
        material = Objects.requireNonNull(materialRepository.save(material));
        return toResponse(material);
    }

    @Transactional
    public MaterialResponse actualizar(Long id, MaterialRequest request, Long usuarioId) {
        Material material = Objects.requireNonNull(
                materialRepository.findByIdAndUsuarioId(
                        Objects.requireNonNull(id), Objects.requireNonNull(usuarioId))
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material no encontrado")));
        mapRequestToEntity(request, material);
        material = Objects.requireNonNull(materialRepository.save(material));
        return toResponse(material);
    }

    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        if (!materialRepository.existsByIdAndUsuarioId(Objects.requireNonNull(id), Objects.requireNonNull(usuarioId))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Material no encontrado");
        }
        materialRepository.deleteById(id);
    }

    private void mapRequestToEntity(MaterialRequest request, Material material) {
        material.setNombre(request.nombre());
        material.setPrecioUnitario(request.precioUnitario());
        material.setUnidadMedida(request.unidadMedida() != null && !request.unidadMedida().isBlank()
                ? request.unidadMedida() : "ud");
    }

    private MaterialResponse toResponse(Material material) {
        return new MaterialResponse(
                material.getId(),
                material.getNombre(),
                material.getPrecioUnitario(),
                material.getUnidadMedida() != null ? material.getUnidadMedida() : "ud"
        );
    }
}
