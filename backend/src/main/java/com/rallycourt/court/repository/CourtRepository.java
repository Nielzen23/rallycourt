package com.rallycourt.court.repository;

import com.rallycourt.court.entity.Court;
import com.rallycourt.court.entity.CourtStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface CourtRepository extends JpaRepository<Court, Long> {

    @Query("""
            select c
            from Court c
            where (:courtTypeCode is null or c.courtType.code = :courtTypeCode)
              and (:venueTypeCode is null or c.venueType.code = :venueTypeCode)
              and (:status is null or c.status = :status)
            """)
    Page<Court> findAllFiltered(
            @Param("courtTypeCode") String courtTypeCode,
            @Param("venueTypeCode") String venueTypeCode,
            @Param("status") CourtStatus status,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Court c where c.id = :id")
    Optional<Court> findByIdForUpdate(@Param("id") Long id);
}
