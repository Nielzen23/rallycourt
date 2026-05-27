package com.rallycourt.auth.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.HandlerMethod;

class AuthExceptionHandlerTest {

    private final AuthExceptionHandler authExceptionHandler = new AuthExceptionHandler();

    @Test
    void handleIllegalArgumentReturnsBadRequest() {
        ResponseEntity<Map<String, String>> response =
                authExceptionHandler.handleIllegalArgument(new IllegalArgumentException("Email already exists"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Email already exists", response.getBody().get("message"));
    }

    @Test
    void handleAccessDeniedReturnsForbidden() {
        ResponseEntity<Map<String, String>> response =
                authExceptionHandler.handleAccessDenied(new AccessDeniedException("Forbidden"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Forbidden", response.getBody().get("message"));
    }

    @Test
    void handleSignupRateLimitReturnsTooManyRequests() {
        ResponseEntity<Map<String, String>> response =
                authExceptionHandler.handleSignupRateLimit(
                        new SignupRateLimitExceededException("Unable to create account with this email")
                );

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("Unable to create account with this email", response.getBody().get("message"));
    }

    @Test
    void handleValidationReturnsFirstFieldErrorMessage() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "Invalid email format"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                new HandlerMethod(this, getClass().getDeclaredMethod("sampleHandler", String.class)).getMethodParameters()[0],
                bindingResult
        );

        ResponseEntity<Map<String, String>> response = authExceptionHandler.handleValidation(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Unable to create account with this email", response.getBody().get("message"));
    }

    @Test
    void handleValidationFallsBackWhenFieldErrorMessageIsNull() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", null, false, null, null, null));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                new HandlerMethod(this, getClass().getDeclaredMethod("sampleHandler", String.class)).getMethodParameters()[0],
                bindingResult
        );

        ResponseEntity<Map<String, String>> response = authExceptionHandler.handleValidation(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Unable to create account with this email", response.getBody().get("message"));
    }

    @Test
    void handleValidationFallsBackWhenNoFieldErrorsExist() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                new HandlerMethod(this, getClass().getDeclaredMethod("sampleHandler", String.class)).getMethodParameters()[0],
                bindingResult
        );

        ResponseEntity<Map<String, String>> response = authExceptionHandler.handleValidation(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Validation failed", response.getBody().get("message"));
    }

    @SuppressWarnings("unused")
    private void sampleHandler(String value) {
    }
}
