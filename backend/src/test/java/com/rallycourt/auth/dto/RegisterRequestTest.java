package com.rallycourt.auth.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RegisterRequestTest {

    @Test
    void recordExposesAllFields() {
        RegisterRequest request = new RegisterRequest(
                "player@rallycourt.local",
                "secret123",
                "Player",
                "One",
                "09123456789"
        );

        assertEquals("player@rallycourt.local", request.email());
        assertEquals("secret123", request.password());
        assertEquals("Player", request.firstName());
        assertEquals("One", request.lastName());
        assertEquals("09123456789", request.mobileNumber());
    }
}
