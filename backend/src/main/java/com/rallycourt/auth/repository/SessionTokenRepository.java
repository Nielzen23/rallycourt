package com.rallycourt.auth.repository;

import com.rallycourt.auth.entity.SessionToken;
import com.rallycourt.auth.entity.User;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionTokenRepository extends JpaRepository<SessionToken, Long> {

    Optional<SessionToken> findByToken(String token);

    void deleteByUser(User user);

    void deleteByExpiresAtBefore(Instant expiresAt);
}
