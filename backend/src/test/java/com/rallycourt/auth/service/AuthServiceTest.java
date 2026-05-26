package com.rallycourt.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rallycourt.auth.dto.AuthResponse;
import com.rallycourt.auth.dto.LoginRequest;
import com.rallycourt.auth.dto.RegisterRequest;
import com.rallycourt.auth.entity.CourtOwnerStatus;
import com.rallycourt.auth.entity.Role;
import com.rallycourt.auth.entity.User;
import com.rallycourt.auth.repository.RoleRepository;
import com.rallycourt.auth.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private Role playerRole;
    private User savedUser;

    @BeforeEach
    void setUp() {
        playerRole = new Role();
        playerRole.setCode("PLAYER");
        playerRole.setName("Player");

        savedUser = new User();
        savedUser.setEmail("player@rallycourt.local");
        savedUser.setFirstName("Player");
        savedUser.setLastName("One");
        savedUser.setRole(playerRole);
        savedUser.setCourtOwnerStatus(CourtOwnerStatus.NONE);
    }

    @Test
    void loginAuthenticatesWithNormalizedEmailAndReturnsResponse() {
        LoginRequest request = new LoginRequest();
        request.setEmail("  Player@RallyCourt.Local ");
        request.setPassword("secret123");

        when(userRepository.findByEmail("player@rallycourt.local")).thenReturn(Optional.of(savedUser));
        when(jwtService.generateToken(savedUser)).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        UsernamePasswordAuthenticationToken token = captor.getValue();
        assertEquals("player@rallycourt.local", token.getPrincipal());
        assertEquals("secret123", token.getCredentials());

        assertEquals("jwt-token", response.token());
        assertEquals("player@rallycourt.local", response.email());
        assertEquals("PLAYER", response.role());
        assertEquals("NONE", response.courtOwnerStatus());
    }

    @Test
    void loginThrowsWhenUserCannotBeLoadedAfterAuthentication() {
        LoginRequest request = new LoginRequest();
        request.setEmail("missing@rallycourt.local");
        request.setPassword("secret123");

        when(userRepository.findByEmail("missing@rallycourt.local")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> authService.login(request)
        );

        assertEquals("User not found: missing@rallycourt.local", exception.getMessage());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                "player@rallycourt.local",
                "secret123",
                "Player",
                "One",
                "09123456789"
        );

        when(userRepository.findByEmail("player@rallycourt.local")).thenReturn(Optional.of(savedUser));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request)
        );

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerRejectsMissingPlayerRole() {
        RegisterRequest request = new RegisterRequest(
                "player@rallycourt.local",
                "secret123",
                "Player",
                "One",
                "09123456789"
        );

        when(userRepository.findByEmail("player@rallycourt.local")).thenReturn(Optional.empty());
        when(roleRepository.findByCode("PLAYER")).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> authService.register(request)
        );

        assertEquals("PLAYER role is not configured", exception.getMessage());
    }

    @Test
    void registerNormalizesAndTrimsFieldsBeforeSaving() {
        RegisterRequest request = new RegisterRequest(
                "  Player@RallyCourt.Local ",
                "secret123",
                "  Player  ",
                "  One  ",
                " 09123456789 "
        );

        when(userRepository.findByEmail("player@rallycourt.local")).thenReturn(Optional.empty());
        when(roleRepository.findByCode("PLAYER")).thenReturn(Optional.of(playerRole));
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("generated-token");

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User user = userCaptor.getValue();

        assertEquals("player@rallycourt.local", user.getEmail());
        assertEquals("Player", user.getFirstName());
        assertEquals("One", user.getLastName());
        assertEquals("09123456789", user.getMobileNumber());
        assertEquals("encoded-password", user.getPassword());
        assertEquals(playerRole, user.getRole());
        assertEquals(CourtOwnerStatus.NONE, user.getCourtOwnerStatus());

        assertNotNull(response);
        assertEquals("generated-token", response.token());
        assertEquals("player@rallycourt.local", response.email());
        assertEquals("Player", response.firstName());
        assertEquals("One", response.lastName());
        assertEquals("PLAYER", response.role());
        assertEquals("NONE", response.courtOwnerStatus());

        verify(roleRepository).findByCode("PLAYER");
        verify(userRepository).findByEmail("player@rallycourt.local");
        verify(passwordEncoder).encode(eq("secret123"));
    }
}
