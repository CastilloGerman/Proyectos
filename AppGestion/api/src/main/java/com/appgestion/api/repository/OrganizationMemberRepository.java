package com.appgestion.api.repository;

import com.appgestion.api.domain.entity.OrganizationMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, Long> {

    List<OrganizationMember> findByUsuario_Id(Long usuarioId);
}
