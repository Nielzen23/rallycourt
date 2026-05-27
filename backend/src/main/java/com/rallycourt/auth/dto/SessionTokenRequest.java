package com.rallycourt.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SessionTokenRequest(
        @NotBlank String sessionToken
) {
}
