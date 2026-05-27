package com.rallycourt.auth.service;

import com.rallycourt.auth.dto.AuthResponse;
import com.rallycourt.auth.dto.LoginRequest;
import com.rallycourt.auth.dto.RegisterRequest;
import com.rallycourt.auth.dto.SessionTokenRequest;
import com.rallycourt.auth.entity.CourtOwnerStatus;
import com.rallycourt.auth.entity.Role;
import com.rallycourt.auth.entity.User;
import com.rallycourt.auth.repository.RoleRepository;
import com.rallycourt.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final String GENERIC_SIGNUP_EMAIL_FAILURE = "Unable to create account with this email";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SignupAttemptService signupAttemptService;
    private final SessionTokenService sessionTokenService;

    public AuthResponse login(LoginRequest request) {
        LOGGER.info("Authenticating user {}", normalizeEmail(request.getEmail()));
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizeEmail(request.getEmail()), request.getPassword())
        );

        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + request.getEmail()));
        LOGGER.info("Authentication succeeded for userId {}", user.getId());
        return authResponseFor(user);
    }

    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        LOGGER.info("Registering user with email {}", normalizedEmail);
        signupAttemptService.checkAllowed(normalizedEmail);

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            signupAttemptService.recordAttempt(normalizedEmail);
            LOGGER.warn("Suspicious signup attempt blocked for existing email {}", normalizedEmail);
            throw new IllegalArgumentException(GENERIC_SIGNUP_EMAIL_FAILURE);
        }

        Role playerRole = roleRepository.findByCode("PLAYER")
                .orElseThrow(() -> new IllegalStateException("PLAYER role is not configured"));

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setMobileNumber(request.mobileNumber().trim());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(playerRole);
        user.setCourtOwnerStatus(CourtOwnerStatus.NONE);

        AuthResponse response = authResponseFor(userRepository.save(user));
        LOGGER.info("Registration completed for user {}", normalizedEmail);
        signupAttemptService.clearAttempts(normalizedEmail);
        return response;
    }

    @Override
    public AuthResponse refresh(SessionTokenRequest request) {
        LOGGER.info("Refreshing session token");
        User user = sessionTokenService.validateSessionToken(request.sessionToken());
        LOGGER.info("Session refresh succeeded for userId {}", user.getId());
        return authResponseFor(user);
    }

    private AuthResponse authResponseFor(User user) {
        return new AuthResponse(
                jwtService.generateToken(user),
                sessionTokenService.issueSessionToken(user),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().getCode(),
                user.getCourtOwnerStatus().name(),
                user.getMobileNumber()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
