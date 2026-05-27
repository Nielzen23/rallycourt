package com.rallycourt.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.rallycourt.auth.entity.CourtOwnerStatus;
import com.rallycourt.auth.entity.Role;
import com.rallycourt.auth.entity.User;
import com.rallycourt.auth.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsernameThrowsWhenUserMissing() {
        when(userRepository.findByEmail("missing@rallycourt.local")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("missing@rallycourt.local")
        );

        assertEquals("User not found: missing@rallycourt.local", exception.getMessage());
    }

    @Test
    void loadUserByUsernameBuildsSpringUserDetails() {
        Role role = new Role();
        role.setCode("ADMIN");
        User user = new User();
        user.setEmail("admin@rallycourt.local");
        user.setPassword("encoded");
        user.setFirstName("Admin");
        user.setLastName("User");
        user.setCourtOwnerStatus(CourtOwnerStatus.NONE);
        user.setRole(role);
        when(userRepository.findByEmail("admin@rallycourt.local")).thenReturn(Optional.of(user));

        var details = customUserDetailsService.loadUserByUsername("admin@rallycourt.local");

        assertEquals("admin@rallycourt.local", details.getUsername());
        assertEquals("encoded", details.getPassword());
        assertEquals(1, details.getAuthorities().size());
    }
}
