package com.rallycourt.activity.aspect;

import com.rallycourt.activity.annotation.ActivityLogAnnotation;
import com.rallycourt.activity.service.ActivityLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ActivityLogAspect {

    private final ActivityLogService activityLogService;

    public ActivityLogAspect(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @Around("@annotation(annotation)")
    public Object logActivity(ProceedingJoinPoint joinPoint, ActivityLogAnnotation annotation) throws Throwable {
        try {
            Object result = joinPoint.proceed();
            activityLogService.log(annotation.value(), "SUCCESS");
            return result;
        } catch (Throwable throwable) {
            activityLogService.log(annotation.value(), "FAIL");
            throw throwable;
        }
    }
}
