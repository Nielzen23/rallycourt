package com.rallycourt.auth.service;

import com.rallycourt.auth.entity.SessionToken;
import com.rallycourt.auth.entity.User;
import com.rallycourt.auth.repository.SessionTokenRepository;
import com.rallycourt.auth.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SessionTokenServiceImpl implements SessionTokenService {

    private static final Duration SESSION_TOKEN_TTL = Duration.ofHours(8);
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionTokenServiceImpl.class);

    private final SessionTokenRepository sessionTokenRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public String issueSessionToken(User user) {
        purgeExpiredTokens();
        LOGGER.info("Issuing session token for userId {}", user.getId());
        sessionTokenRepository.deleteByUser(user);

        SessionToken sessionToken = new SessionToken();
        sessionToken.setToken(UUID.randomUUID().toString());
        sessionToken.setUser(user);
        sessionToken.setExpiresAt(Instant.now().plus(SESSION_TOKEN_TTL));
        return sessionTokenRepository.save(sessionToken).getToken();
    }

    @Override
    @Transactional
    public User validateSessionToken(String sessionToken) {
        purgeExpiredTokens();
        LOGGER.debug("Validating session token");
        SessionToken record = sessionTokenRepository.findByToken(sessionToken)
                .orElseThrow(() -> new AccessDeniedException("Session token is invalid or expired"));
        if (record.getExpiresAt().isBefore(Instant.now())) {
            sessionTokenRepository.delete(record);
            LOGGER.info("Session token expired for userId {}", record.getUser().getId());
            throw new AccessDeniedException("Session token is invalid or expired");
        }

        sessionTokenRepository.delete(record);
        LOGGER.info("Session token accepted for userId {}", record.getUser().getId());
        return userRepository.findById(record.getUser().getId())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));
    }

    private void purgeExpiredTokens() {
        LOGGER.debug("Purging expired session tokens");
        sessionTokenRepository.deleteByExpiresAtBefore(Instant.now());
    }
}
