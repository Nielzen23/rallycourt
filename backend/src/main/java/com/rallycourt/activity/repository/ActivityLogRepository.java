package com.rallycourt.activity.repository;

import com.rallycourt.activity.entity.ActivityLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ActivityLogRepository extends MongoRepository<ActivityLog, String> {
}
