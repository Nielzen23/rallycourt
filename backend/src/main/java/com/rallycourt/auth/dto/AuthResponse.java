package com.rallycourt.auth.dto;

public record AuthResponse(
        String token,
        String sessionToken,
        String email,
        String firstName,
        String lastName,
        String role,
        String courtOwnerStatus,
        String mobileNumber
) {
}
