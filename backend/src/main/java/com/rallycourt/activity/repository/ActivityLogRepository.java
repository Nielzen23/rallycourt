package com.rallycourt.activity.repository;

import com.rallycourt.activity.entity.ActivityLog;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ActivityLogRepository extends MongoRepository<ActivityLog, String> {

    List<ActivityLog> findByActorIn(Collection<String> actors);

    Page<ActivityLog> findByActorOrderByCreatedAtDesc(String actor, Pageable pageable);
}
