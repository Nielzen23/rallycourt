package com.rallycourt.activity.aspect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rallycourt.activity.annotation.ActivityLogAnnotation;
import com.rallycourt.activity.service.ActivityLogService;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
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
    private ProceedingJoinPoint joinPoint;

    @InjectMocks
    private ActivityLogAspect activityLogAspect;

    @Test
    void logActivityPersistsSuccessWhenNoExceptionThrown() throws Throwable {
        when(joinPoint.proceed()).thenReturn("ok");

        Method method = TestAnnotatedMethods.class.getDeclaredMethod("login");
        ActivityLogAnnotation annotation = method.getAnnotation(ActivityLogAnnotation.class);

        Object result = activityLogAspect.logActivity(joinPoint, annotation);

        assertEquals("ok", result);
        verify(activityLogService).log("LOGIN_SUCCESS", "SUCCESS");
    }

    @Test
    void logActivityPersistsFailWhenExceptionThrown() throws Throwable {
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("boom"));

        Method method = TestAnnotatedMethods.class.getDeclaredMethod("login");
        ActivityLogAnnotation annotation = method.getAnnotation(ActivityLogAnnotation.class);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> activityLogAspect.logActivity(joinPoint, annotation)
        );

        assertEquals("boom", exception.getMessage());
        verify(activityLogService).log("LOGIN_SUCCESS", "FAIL");
    }

    private static class TestAnnotatedMethods {

        @ActivityLogAnnotation("LOGIN_SUCCESS")
        void login() {
        }
    }
}
