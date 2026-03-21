package com.appgestion.api.repository;

import com.appgestion.api.domain.entity.Invitacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvitacionRepository extends JpaRepository<Invitacion, Long> {
    Optional<Invitacion> findByTokenHash(String tokenHash);
}
