package com.rallycourt.activity.aspect;

import com.rallycourt.activity.annotation.ActivityLogAnnotation;
import com.rallycourt.activity.service.ActivityLogService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ActivityLogAspect {

    private final ActivityLogService activityLogService;

    public ActivityLogAspect(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @AfterReturning("@annotation(annotation)")
    public void logActivity(JoinPoint joinPoint, ActivityLogAnnotation annotation) {
        activityLogService.log(annotation.value(), joinPoint.getSignature().toShortString());
    }
}
