package com.rallycourt.auth.service;

import com.rallycourt.auth.exception.SignupRateLimitExceededException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SignupAttemptServiceImpl implements SignupAttemptService {

    private static final int MAX_ATTEMPTS_PER_WINDOW = 5;
    private static final Duration ATTEMPT_WINDOW = Duration.ofMinutes(15);
    private static final Logger LOGGER = LoggerFactory.getLogger(SignupAttemptServiceImpl.class);

    private final Map<String, SignupAttemptWindow> attemptsByEmail = new ConcurrentHashMap<>();

    public void checkAllowed(String normalizedEmail) {
        SignupAttemptWindow window = attemptsByEmail.get(normalizedEmail);

        if (window == null) {
            return;
        }

        if (window.isExpired()) {
            attemptsByEmail.remove(normalizedEmail, window);
            LOGGER.debug("Expired signup attempt window cleared for email {}", normalizedEmail);
            return;
        }

        if (window.count() >= MAX_ATTEMPTS_PER_WINDOW) {
            LOGGER.warn("Signup rate limit exceeded for email {}", normalizedEmail);
            throw new SignupRateLimitExceededException("Unable to create account with this email");
        }
    }

    public void recordAttempt(String normalizedEmail) {
        LOGGER.info("Recording signup attempt for email {}", normalizedEmail);
        attemptsByEmail.compute(normalizedEmail, (email, current) -> {
            if (current == null || current.isExpired()) {
                return new SignupAttemptWindow(1, Instant.now());
            }

            return new SignupAttemptWindow(current.count() + 1, current.windowStartedAt());
        });
    }

    public void clearAttempts(String normalizedEmail) {
        LOGGER.debug("Clearing signup attempts for email {}", normalizedEmail);
        attemptsByEmail.remove(normalizedEmail);
    }

    private record SignupAttemptWindow(int count, Instant windowStartedAt) {

        private boolean isExpired() {
            return windowStartedAt.plus(ATTEMPT_WINDOW).isBefore(Instant.now());
        }
    }
}
