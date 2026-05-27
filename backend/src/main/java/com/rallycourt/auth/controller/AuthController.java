package com.rallycourt.auth.controller;

import com.rallycourt.activity.annotation.ActivityLogAnnotation;
import com.rallycourt.auth.dto.AuthResponse;
import com.rallycourt.auth.dto.LoginRequest;
import com.rallycourt.auth.dto.RegisterRequest;
import com.rallycourt.auth.dto.SessionTokenRequest;
import com.rallycourt.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    @PostMapping("/login")
    @ActivityLogAnnotation("LOGIN_SUCCESS")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        LOGGER.info("Auth login requested for email {}", request.getEmail());
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    @ActivityLogAnnotation("USER_REGISTERED")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        LOGGER.info("Auth registration requested for email {}", request.email());
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/refresh")
    @ActivityLogAnnotation("SESSION_RENEWED")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody SessionTokenRequest request) {
        LOGGER.info("Session renewal requested");
        return ResponseEntity.ok(authService.refresh(request));
    }
}
