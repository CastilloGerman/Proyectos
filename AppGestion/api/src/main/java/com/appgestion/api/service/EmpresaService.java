package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Empresa;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.dto.request.EmpresaRequest;
import com.appgestion.api.dto.response.EmpresaResponse;
import com.appgestion.api.repository.EmpresaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepository;

    public EmpresaService(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    public EmpresaResponse obtenerPorUsuario(Long usuarioId) {
        Empresa emp = empresaRepository.findByUsuarioId(usuarioId).orElse(null);
        return toResponse(emp);
    }

    @Transactional
    public EmpresaResponse guardar(EmpresaRequest request, Usuario usuario) {
        Empresa emp = empresaRepository.findByUsuarioId(usuario.getId())
                .orElseGet(() -> {
                    Empresa e = new Empresa();
                    e.setUsuario(usuario);
                    return e;
                });
        emp.setNombre(request.nombre() != null ? request.nombre() : "");
        emp.setDireccion(request.direccion());
        emp.setNif(request.nif());
        emp.setTelefono(request.telefono());
        emp.setEmail(request.email());
        emp.setNotasPiePresupuesto(request.notasPiePresupuesto());
        emp.setNotasPieFactura(request.notasPieFactura());
        emp = empresaRepository.save(emp);
        return toResponse(emp);
    }

    public Empresa getEmpresaOrNull(Long usuarioId) {
        return empresaRepository.findByUsuarioId(usuarioId).orElse(null);
    }

    private EmpresaResponse toResponse(Empresa emp) {
        if (emp == null) {
            return new EmpresaResponse(null, "", null, null, null, null, null, null);
        }
        return new EmpresaResponse(
                emp.getId(),
                emp.getNombre(),
                emp.getDireccion(),
                emp.getNif(),
                emp.getTelefono(),
                emp.getEmail(),
                emp.getNotasPiePresupuesto(),
                emp.getNotasPieFactura()
        );
    }
}
