package com.rallycourt.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 100, message = "Invalid email format")
        String email,
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,
        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name is required")
        @Pattern(regexp = "^[A-Za-z][A-Za-z '\\-]*$", message = "First name is required")
        String firstName,
        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name is required")
        @Pattern(regexp = "^[A-Za-z][A-Za-z '\\-]*$", message = "Last name is required")
        String lastName,
        @NotBlank(message = "Invalid mobile number")
        @Pattern(regexp = "^09\\d{9}$", message = "Invalid mobile number")
        String mobileNumber
) {
}
