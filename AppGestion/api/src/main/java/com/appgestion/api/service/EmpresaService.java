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
        emp.setCodigoPostal(request.codigoPostal());
        emp.setProvincia(request.provincia());
        emp.setPais(request.pais() != null ? request.pais() : "España");
        emp.setNif(request.nif());
        emp.setTelefono(request.telefono());
        emp.setEmail(request.email());
        emp.setNotasPiePresupuesto(request.notasPiePresupuesto());
        emp.setNotasPieFactura(request.notasPieFactura());
        if (request.mailHost() != null) emp.setMailHost(request.mailHost());
        if (request.mailPort() != null) emp.setMailPort(request.mailPort());
        if (request.mailUsername() != null) emp.setMailUsername(request.mailUsername());
        if (request.mailPassword() != null && !request.mailPassword().isBlank()) emp.setMailPassword(request.mailPassword());
        emp = empresaRepository.save(emp);
        return toResponse(emp);
    }

    public Empresa getEmpresaOrNull(Long usuarioId) {
        return empresaRepository.findByUsuarioId(usuarioId).orElse(null);
    }

    private EmpresaResponse toResponse(Empresa emp) {
        if (emp == null) {
            return new EmpresaResponse(null, "", null, null, null, null, null, null, null, null, null, null, null, null, false);
        }
        boolean mailConfigurado = emp.getMailUsername() != null && !emp.getMailUsername().isBlank()
                && emp.getMailPassword() != null && !emp.getMailPassword().isBlank();
        return new EmpresaResponse(
                emp.getId(),
                emp.getNombre(),
                emp.getDireccion(),
                emp.getCodigoPostal(),
                emp.getProvincia(),
                emp.getPais(),
                emp.getNif(),
                emp.getTelefono(),
                emp.getEmail(),
                emp.getNotasPiePresupuesto(),
                emp.getNotasPieFactura(),
                emp.getMailHost(),
                emp.getMailPort(),
                emp.getMailUsername(),
                mailConfigurado
        );
    }
}
