package com.rallycourt.activity.aspect;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rallycourt.activity.annotation.ActivityLogAnnotation;
import com.rallycourt.activity.service.ActivityLogService;
import java.lang.reflect.Method;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivityLogAspectTest {

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private Signature signature;

    @InjectMocks
    private ActivityLogAspect activityLogAspect;

    @Test
    void logActivityDelegatesAnnotationValueAndSignature() throws NoSuchMethodException {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("AuthController.login(..)");

        Method method = TestAnnotatedMethods.class.getDeclaredMethod("login");
        ActivityLogAnnotation annotation = method.getAnnotation(ActivityLogAnnotation.class);

        activityLogAspect.logActivity(joinPoint, annotation);

        verify(activityLogService).log("LOGIN_SUCCESS", "AuthController.login(..)");
    }

    private static class TestAnnotatedMethods {

        @ActivityLogAnnotation("LOGIN_SUCCESS")
        void login() {
        }
    }
}
