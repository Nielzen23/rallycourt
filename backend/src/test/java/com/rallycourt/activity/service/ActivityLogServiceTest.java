package com.rallycourt.activity.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.rallycourt.activity.entity.ActivityLog;
import com.rallycourt.activity.repository.ActivityLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivityLogServiceTest {

    @Mock
    private ActivityLogRepository activityLogRepository;

    @InjectMocks
    private ActivityLogService activityLogService;

    @Test
    void logPersistsActionAndActor() {
        activityLogService.log("LOGIN_SUCCESS", "AuthController#login(..)");

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());

        ActivityLog savedLog = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("LOGIN_SUCCESS", savedLog.getAction());
        org.junit.jupiter.api.Assertions.assertEquals("AuthController#login(..)", savedLog.getActor());
        org.junit.jupiter.api.Assertions.assertNotNull(savedLog.getCreatedAt());
    }

    @Test
    void logSwallowsRepositoryFailure() {
        doThrow(new RuntimeException("Mongo unavailable"))
                .when(activityLogRepository)
                .save(any(ActivityLog.class));

        activityLogService.log("PAYMENT_SUCCESS", "PaymentController#processPayment(..)");
    }
}
