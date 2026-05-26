package com.rallycourt.activity.service;

import com.rallycourt.activity.entity.ActivityLog;
import com.rallycourt.activity.repository.ActivityLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ActivityLogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityLogService.class);

    private final ActivityLogRepository activityLogRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    public void log(String action, String actor) {
        ActivityLog entry = new ActivityLog();
        entry.setAction(action);
        entry.setActor(actor);
        try {
            activityLogRepository.save(entry);
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to persist activity log for action {}", action, exception);
        }
    }
}
