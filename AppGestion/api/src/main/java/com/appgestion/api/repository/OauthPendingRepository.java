package com.appgestion.api.repository;

import com.appgestion.api.domain.entity.OauthPending;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface OauthPendingRepository extends JpaRepository<OauthPending, Long> {

    Optional<OauthPending> findByStateToken(String stateToken);

    @Modifying
    @Query("DELETE FROM OauthPending o WHERE o.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
