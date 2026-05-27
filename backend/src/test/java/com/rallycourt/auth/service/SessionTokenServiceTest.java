package com.rallycourt.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rallycourt.auth.entity.CourtOwnerStatus;
import com.rallycourt.auth.entity.Role;
import com.rallycourt.auth.entity.SessionToken;
import com.rallycourt.auth.entity.User;
import com.rallycourt.auth.repository.SessionTokenRepository;
import com.rallycourt.auth.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class SessionTokenServiceTest {

    @Mock
    private SessionTokenRepository sessionTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SessionTokenServiceImpl sessionTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setCode("PLAYER");
        user = new User();
        user.setId(10L);
        user.setEmail("player@rallycourt.local");
        user.setCourtOwnerStatus(CourtOwnerStatus.NONE);
        user.setRole(role);
    }

    @Test
    void issueSessionTokenDeletesExistingUserTokensAndPersistsNewToken() {
        when(sessionTokenRepository.save(any(SessionToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String token = sessionTokenService.issueSessionToken(user);

        ArgumentCaptor<SessionToken> captor = ArgumentCaptor.forClass(SessionToken.class);
        verify(sessionTokenRepository).deleteByUser(user);
        verify(sessionTokenRepository).save(captor.capture());
        assertNotNull(token);
        assertEquals(user, captor.getValue().getUser());
    }

    @Test
    void validateSessionTokenReturnsUserAndDeletesToken() {
        SessionToken sessionToken = new SessionToken();
        sessionToken.setToken("session-token");
        sessionToken.setUser(user);
        sessionToken.setExpiresAt(Instant.now().plusSeconds(60));

        when(sessionTokenRepository.findByToken("session-token")).thenReturn(Optional.of(sessionToken));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        User result = sessionTokenService.validateSessionToken("session-token");

        assertEquals(user, result);
        verify(sessionTokenRepository).delete(sessionToken);
    }

    @Test
    void validateSessionTokenRejectsExpiredToken() {
        SessionToken sessionToken = new SessionToken();
        sessionToken.setToken("session-token");
        sessionToken.setUser(user);
        sessionToken.setExpiresAt(Instant.now().minusSeconds(60));

        when(sessionTokenRepository.findByToken("session-token")).thenReturn(Optional.of(sessionToken));

        assertThrows(AccessDeniedException.class, () -> sessionTokenService.validateSessionToken("session-token"));
        verify(sessionTokenRepository).delete(sessionToken);
    }
}
