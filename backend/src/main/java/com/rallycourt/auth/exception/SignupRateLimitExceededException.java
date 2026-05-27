package com.rallycourt.auth.exception;

public class SignupRateLimitExceededException extends RuntimeException {

    public SignupRateLimitExceededException(String message) {
        super(message);
    }
}
