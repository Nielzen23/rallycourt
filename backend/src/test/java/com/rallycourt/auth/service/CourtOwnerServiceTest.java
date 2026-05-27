package com.rallycourt.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rallycourt.auth.entity.CourtOwnerStatus;
import com.rallycourt.auth.entity.Role;
import com.rallycourt.auth.entity.User;
import com.rallycourt.auth.repository.RoleRepository;
import com.rallycourt.auth.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CourtOwnerServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private CourtOwnerServiceImpl courtOwnerService;

    private Role playerRole;
    private Role ownerRole;

    @BeforeEach
    void setUp() {
        playerRole = role("PLAYER");
        ownerRole = role("COURT_OWNER");
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void applyMarksEligiblePlayerAsPending() {
        User user = user("player@rallycourt.local", playerRole, CourtOwnerStatus.NONE);
        authenticate(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User result = courtOwnerService.apply();

        assertSame(user, result);
        assertEquals(CourtOwnerStatus.PENDING, user.getCourtOwnerStatus());
        verify(userRepository).save(user);
    }

    @Test
    void applyAllowsRejectedPlayerToReapply() {
        User user = user("player@rallycourt.local", playerRole, CourtOwnerStatus.REJECTED);
        authenticate(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User result = courtOwnerService.apply();

        assertSame(user, result);
        assertEquals(CourtOwnerStatus.PENDING, user.getCourtOwnerStatus());
    }

    @Test
    void applyRejectsNonPlayerRole() {
        User user = user("owner@rallycourt.local", ownerRole, CourtOwnerStatus.NONE);
        authenticate(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, courtOwnerService::apply);

        assertEquals("Only players can apply as court owner", exception.getMessage());
    }

    @Test
    void applyRejectsPendingOrApprovedState() {
        User user = user("player@rallycourt.local", playerRole, CourtOwnerStatus.APPROVED);
        authenticate(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, courtOwnerService::apply);

        assertEquals("Court owner application is not allowed in current state", exception.getMessage());
    }

    @Test
    void applyRejectsMissingAuthentication() {
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, courtOwnerService::apply);

        assertEquals("Authentication is required", exception.getMessage());
    }

    @Test
    void applyRejectsMissingAuthenticatedUser() {
        authenticate("missing@rallycourt.local");
        when(userRepository.findByEmail("missing@rallycourt.local")).thenReturn(Optional.empty());

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, courtOwnerService::apply);

        assertEquals("Authenticated user not found", exception.getMessage());
    }

    @Test
    void approvePromotesPendingUserToCourtOwner() {
        User user = user("player@rallycourt.local", playerRole, CourtOwnerStatus.PENDING);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(roleRepository.findByCode("COURT_OWNER")).thenReturn(Optional.of(ownerRole));
        when(userRepository.save(user)).thenReturn(user);

        User result = courtOwnerService.approve(7L);

        assertSame(user, result);
        assertEquals(ownerRole, user.getRole());
        assertEquals(CourtOwnerStatus.APPROVED, user.getCourtOwnerStatus());
    }

    @Test
    void approveRejectsMissingUser() {
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> courtOwnerService.approve(7L));

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void approveRejectsNonPendingUser() {
        User user = user("player@rallycourt.local", playerRole, CourtOwnerStatus.NONE);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> courtOwnerService.approve(7L));

        assertEquals("Only pending applications can be approved", exception.getMessage());
    }

    @Test
    void approveRejectsMissingCourtOwnerRole() {
        User user = user("player@rallycourt.local", playerRole, CourtOwnerStatus.PENDING);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(roleRepository.findByCode("COURT_OWNER")).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> courtOwnerService.approve(7L));

        assertEquals("COURT_OWNER role is not configured", exception.getMessage());
    }

    @Test
    void rejectMarksPendingUserRejected() {
        User user = user("player@rallycourt.local", playerRole, CourtOwnerStatus.PENDING);
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User result = courtOwnerService.reject(9L);

        assertSame(user, result);
        assertEquals(CourtOwnerStatus.REJECTED, user.getCourtOwnerStatus());
        verify(userRepository).save(user);
    }

    @Test
    void rejectRejectsMissingUser() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> courtOwnerService.reject(9L));

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void rejectRejectsNonPendingUser() {
        User user = user("player@rallycourt.local", playerRole, CourtOwnerStatus.REJECTED);
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> courtOwnerService.reject(9L));

        assertEquals("Only pending applications can be rejected", exception.getMessage());
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "ignored")
        );
    }

    private Role role(String code) {
        Role role = new Role();
        role.setCode(code);
        role.setName(code);
        return role;
    }

    private User user(String email, Role role, CourtOwnerStatus status) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(role);
        user.setCourtOwnerStatus(status);
        return user;
    }
}
