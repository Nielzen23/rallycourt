package com.rallycourt.auth.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.rallycourt.auth.exception.SignupRateLimitExceededException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SignupAttemptServiceTest {

    private final SignupAttemptServiceImpl service = new SignupAttemptServiceImpl();

    @Test
    void checkAllowedDoesNothingWhenNoAttemptsExist() {
        assertDoesNotThrow(() -> service.checkAllowed("user@rallycourt.local"));
    }

    @Test
    void recordAttemptStartsNewWindowAndClearRemovesIt() {
        service.recordAttempt("user@rallycourt.local");
        service.clearAttempts("user@rallycourt.local");

        assertDoesNotThrow(() -> service.checkAllowed("user@rallycourt.local"));
    }

    @Test
    void checkAllowedClearsExpiredWindow() throws Exception {
        putAttemptWindow("expired@rallycourt.local", 5, Instant.now().minus(Duration.ofMinutes(16)));

        assertDoesNotThrow(() -> service.checkAllowed("expired@rallycourt.local"));
    }

    @Test
    void checkAllowedThrowsWhenWindowReachesLimit() throws Exception {
        putAttemptWindow("limited@rallycourt.local", 5, Instant.now());

        assertThrows(SignupRateLimitExceededException.class, () -> service.checkAllowed("limited@rallycourt.local"));
    }

    @Test
    void recordAttemptResetsExpiredWindow() throws Exception {
        putAttemptWindow("expired@rallycourt.local", 4, Instant.now().minus(Duration.ofMinutes(16)));

        service.recordAttempt("expired@rallycourt.local");

        assertDoesNotThrow(() -> service.checkAllowed("expired@rallycourt.local"));
    }

    @Test
    void recordAttemptIncrementsActiveWindow() throws Exception {
        putAttemptWindow("active@rallycourt.local", 4, Instant.now());

        service.recordAttempt("active@rallycourt.local");

        assertThrows(SignupRateLimitExceededException.class, () -> service.checkAllowed("active@rallycourt.local"));
    }

    @SuppressWarnings("unchecked")
    private void putAttemptWindow(String email, int count, Instant startedAt) throws Exception {
        Field attemptsField = SignupAttemptServiceImpl.class.getDeclaredField("attemptsByEmail");
        attemptsField.setAccessible(true);
        Map<String, Object> attempts = (Map<String, Object>) attemptsField.get(service);

        Class<?> windowClass = Class.forName("com.rallycourt.auth.service.SignupAttemptServiceImpl$SignupAttemptWindow");
        Constructor<?> constructor = windowClass.getDeclaredConstructor(int.class, Instant.class);
        constructor.setAccessible(true);
        attempts.put(email, constructor.newInstance(count, startedAt));
    }
}
