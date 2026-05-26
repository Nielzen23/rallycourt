package com.rallycourt.reservation.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ReservationExceptionHandler {

    @ExceptionHandler(ReservationNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ReservationNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler({ReservationValidationException.class, ConstraintViolationException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(Exception exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(ReservationConflictException.class)
    public ResponseEntity<Map<String, String>> handleConflict(ReservationConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", exception.getMessage()));
    }
}
