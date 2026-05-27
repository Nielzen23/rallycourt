package com.rallycourt.auth.service;

public interface SignupAttemptService {

    void checkAllowed(String normalizedEmail);

    void recordAttempt(String normalizedEmail);

    void clearAttempts(String normalizedEmail);
}
