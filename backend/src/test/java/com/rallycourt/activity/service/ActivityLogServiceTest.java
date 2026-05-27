package com.rallycourt.activity.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rallycourt.activity.entity.ActivityLog;
import com.rallycourt.activity.repository.ActivityLogRepository;
import com.rallycourt.auth.entity.User;
import com.rallycourt.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.AfterEach;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.TestingAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class ActivityLogServiceTest {

    @Mock
    private ActivityLogRepository activityLogRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ActivityLogServiceImpl activityLogService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void logPersistsActionStatusAndActor() {
        User user = new User();
        user.setId(42L);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("adminrallycourt@rallycourt.local", "ignored", "ROLE_ADMIN")
        );
        when(userRepository.findByEmail("adminrallycourt@rallycourt.local")).thenReturn(java.util.Optional.of(user));

        activityLogService.log("LOGIN_SUCCESS", "SUCCESS");

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());

        ActivityLog savedLog = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("LOGIN_SUCCESS", savedLog.getAction());
        org.junit.jupiter.api.Assertions.assertEquals("SUCCESS", savedLog.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("42", savedLog.getActor());
        org.junit.jupiter.api.Assertions.assertNotNull(savedLog.getCreatedAt());
    }

    @Test
    void logSwallowsRepositoryFailure() {
        doThrow(new RuntimeException("Mongo unavailable"))
                .when(activityLogRepository)
                .save(any(ActivityLog.class));

        activityLogService.log("PAYMENT_SUCCESS", "FAIL");
    }
}
