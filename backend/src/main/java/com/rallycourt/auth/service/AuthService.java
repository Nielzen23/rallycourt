package com.rallycourt.auth.service;

import com.rallycourt.auth.dto.AuthResponse;
import com.rallycourt.auth.dto.LoginRequest;
import com.rallycourt.auth.dto.RegisterRequest;
import com.rallycourt.auth.entity.CourtOwnerStatus;
import com.rallycourt.auth.entity.Role;
import com.rallycourt.auth.entity.User;
import com.rallycourt.auth.repository.RoleRepository;
import com.rallycourt.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizeEmail(request.getEmail()), request.getPassword())
        );

        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + request.getEmail()));
        return authResponseFor(user);
    }

    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
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

        return authResponseFor(userRepository.save(user));
    }

    private AuthResponse authResponseFor(User user) {
        return new AuthResponse(
                jwtService.generateToken(user),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().getCode(),
                user.getCourtOwnerStatus().name()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
