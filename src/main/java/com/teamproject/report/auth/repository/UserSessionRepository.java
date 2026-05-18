package com.teamproject.report.auth.repository;

import com.teamproject.report.auth.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findByAccessToken(String accessToken);

    void deleteAllByUserId(UUID userId);
}
