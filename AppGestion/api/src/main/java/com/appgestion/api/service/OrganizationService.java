package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Organization;
import com.appgestion.api.domain.entity.OrganizationMember;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.repository.OrganizationMemberRepository;
import com.appgestion.api.repository.OrganizationRepository;
import com.appgestion.api.repository.UsuarioRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Provisiona organización (tenant) y membresías. Cada cuenta nueva tiene un workspace "Personal".
 * Los enlaces de referido crean cuenta independiente (no comparten organización con quien refiere).
 */
@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final UsuarioRepository usuarioRepository;

    public OrganizationService(OrganizationRepository organizationRepository,
                                 OrganizationMemberRepository organizationMemberRepository,
                                 UsuarioRepository usuarioRepository) {
        this.organizationRepository = organizationRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Crea organización "Personal", persiste el usuario con {@code organization_id} y fila de miembro OWNER.
     * El usuario puede ser transiente (aún sin id).
     */
    @Transactional
    public Usuario attachNewPersonalOrganization(Usuario usuario) {
        Organization org = new Organization();
        org.setName("Personal");
        org = organizationRepository.save(org);
        usuario.setOrganization(org);
        usuario = usuarioRepository.save(usuario);

        OrganizationMember member = new OrganizationMember();
        member.setOrganization(org);
        member.setUsuario(usuario);
        member.setRole("OWNER");
        organizationMemberRepository.save(member);
        return usuario;
    }

    /**
     * Organización del invitador (crea workspace personal si faltara, p. ej. datos previos a migración).
     */
    @Transactional
    public Organization resolveOrganizationForInviter(@NonNull Long inviterUsuarioId) {
        Long inviterId = Objects.requireNonNull(inviterUsuarioId, "inviterUsuarioId");
        Usuario inviter = usuarioRepository.findById(inviterId)
                .orElseThrow(() -> new IllegalStateException("Usuario invitador no encontrado"));
        if (inviter.getOrganization() == null) {
            attachNewPersonalOrganization(inviter);
            inviter = usuarioRepository.findById(inviterId).orElseThrow();
        }
        return inviter.getOrganization();
    }

    @Transactional
    public void addMember(Organization organization, Usuario usuario, String role) {
        OrganizationMember member = new OrganizationMember();
        member.setOrganization(organization);
        member.setUsuario(usuario);
        member.setRole(role != null && !role.isBlank() ? role : "MEMBER");
        organizationMemberRepository.save(member);
    }
}
