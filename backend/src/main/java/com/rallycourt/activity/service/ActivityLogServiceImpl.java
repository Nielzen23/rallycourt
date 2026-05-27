package com.rallycourt.activity.service;

import com.rallycourt.activity.entity.ActivityLog;
import com.rallycourt.activity.repository.ActivityLogRepository;
import com.rallycourt.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class ActivityLogServiceImpl implements ActivityLogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityLogServiceImpl.class);
    private static final String SYSTEM_ACTOR = "system";

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;

    public ActivityLogServiceImpl(ActivityLogRepository activityLogRepository, UserRepository userRepository) {
        this.activityLogRepository = activityLogRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void log(String action, String status) {
        ActivityLog entry = new ActivityLog();
        entry.setAction(action);
        entry.setStatus(status);
        entry.setActor(resolveActor());
        try {
            activityLogRepository.save(entry);
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to persist activity log for action {} with status {}", action, status, exception);
        }
    }

    private String resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || !authentication.isAuthenticated()) {
            return SYSTEM_ACTOR;
        }

        return userRepository.findByEmail(authentication.getName())
                .map(user -> String.valueOf(user.getId()))
                .orElse(SYSTEM_ACTOR);
    }
}
