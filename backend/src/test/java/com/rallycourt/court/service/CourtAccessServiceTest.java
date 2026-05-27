package com.rallycourt.court.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.rallycourt.auth.entity.Role;
import com.rallycourt.auth.entity.User;
import com.rallycourt.auth.repository.UserRepository;
import com.rallycourt.court.entity.Court;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CourtAccessServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CourtAccessServiceImpl courtAccessService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserRejectsMissingAuthentication() {
        assertThrows(AccessDeniedException.class, () -> courtAccessService.getCurrentUser());
    }

    @Test
    void getCurrentUserRejectsAuthenticationWithoutName() {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.getName()).thenReturn(null);

        assertThrows(AccessDeniedException.class, () -> courtAccessService.getCurrentUser());
    }

    @Test
    void getCurrentUserRejectsMissingRepositoryUser() {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.getName()).thenReturn("ghost@rallycourt.local");
        when(userRepository.findByEmail("ghost@rallycourt.local")).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> courtAccessService.getCurrentUser());
    }

    @Test
    void getCurrentUserReturnsAuthenticatedUser() {
        User user = userWithRole(1L, "adminrallycourt@rallycourt.local", "ADMIN");
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.getName()).thenReturn("adminrallycourt@rallycourt.local");
        when(userRepository.findByEmail("adminrallycourt@rallycourt.local")).thenReturn(Optional.of(user));

        User currentUser = courtAccessService.getCurrentUser();

        assertEquals("adminrallycourt@rallycourt.local", currentUser.getEmail());
    }

    @Test
    void verifyCanCreateCourtAllowsAdmin() {
        User user = userWithRole(1L, "AdminRallyCourt", "ADMIN");

        assertDoesNotThrow(() -> courtAccessService.verifyCanCreateCourt(user));
    }

    @Test
    void verifyCanCreateCourtAllowsCourtOwner() {
        User user = userWithRole(2L, "CourtOwnerOne", "COURT_OWNER");

        assertDoesNotThrow(() -> courtAccessService.verifyCanCreateCourt(user));
    }

    @Test
    void verifyCanCreateCourtRejectsOtherRoles() {
        User user = userWithRole(3L, "PlayerOne", "PLAYER");

        assertThrows(AccessDeniedException.class, () -> courtAccessService.verifyCanCreateCourt(user));
    }

    @Test
    void verifyCanUpdateCourtAllowsAdmin() {
        User admin = userWithRole(1L, "AdminRallyCourt", "ADMIN");
        Court court = courtOwnedBy(2L);

        assertDoesNotThrow(() -> courtAccessService.verifyCanUpdateCourt(admin, court));
    }

    @Test
    void verifyCanUpdateCourtAllowsOwner() {
        User owner = userWithRole(2L, "CourtOwnerOne", "COURT_OWNER");
        Court court = courtOwnedBy(2L);

        assertDoesNotThrow(() -> courtAccessService.verifyCanUpdateCourt(owner, court));
    }

    @Test
    void verifyCanUpdateCourtRejectsNonOwner() {
        User owner = userWithRole(2L, "CourtOwnerOne", "COURT_OWNER");
        User otherUser = userWithRole(3L, "OtherOwner", "COURT_OWNER");
        Court court = new Court();
        court.setOwner(owner);

        assertThrows(AccessDeniedException.class, () -> courtAccessService.verifyCanUpdateCourt(otherUser, court));
    }

    private User userWithRole(Long id, String email, String roleCode) {
        Role role = new Role();
        role.setCode(roleCode);
        role.setName(roleCode);

        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setMobileNumber("09170000000");
        user.setRole(role);
        return user;
    }

    private Court courtOwnedBy(Long ownerId) {
        User owner = new User();
        owner.setId(ownerId);

        Court court = new Court();
        court.setOwner(owner);
        return court;
    }
}
