package com.rallycourt.reservation.repository;

import com.rallycourt.reservation.entity.Reservation;
import com.rallycourt.reservation.entity.ReservationStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByReservedByOrderByStartTimeDesc(String reservedBy);

    Page<Reservation> findByReservedBy(String reservedBy, Pageable pageable);

    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, LocalDateTime expiresAt);

    boolean existsByCourtIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
            Long courtId,
            Collection<ReservationStatus> statuses,
            LocalDateTime endTime,
            LocalDateTime startTime
    );

    boolean existsByCourtIdAndStatusInAndStartTimeGreaterThan(
            Long courtId,
            Collection<ReservationStatus> statuses,
            LocalDateTime startTime
    );

    List<Reservation> findByCourtIdAndStartTimeGreaterThanEqualOrderByStartTimeAsc(
            Long courtId,
            LocalDateTime startTime
    );

    List<Reservation> findByCourtIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThanOrderByStartTimeAsc(
            Long courtId,
            Collection<ReservationStatus> statuses,
            LocalDateTime rangeEnd,
            LocalDateTime rangeStart
    );
}
