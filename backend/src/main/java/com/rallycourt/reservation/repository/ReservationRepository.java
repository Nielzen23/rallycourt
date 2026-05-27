package com.rallycourt.reservation.repository;

import com.rallycourt.reservation.entity.Reservation;
import com.rallycourt.reservation.entity.ReservationStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    @Query("""
            select distinct r.reservedBy
            from Reservation r
            where r.reservedBy is not null
            """)
    List<String> findDistinctReservedBy();

    @Query("""
            select c.name as label, count(r.id) as count
            from Reservation r
            join Court c on c.id = r.courtId
            group by c.id, c.name
            order by count(r.id) desc, c.name asc
            """)
    List<BookingCountProjection> findTopCourtsByBookings(Pageable pageable);

    @Query("""
            select c.courtType.code as label, count(r.id) as count
            from Reservation r
            join Court c on c.id = r.courtId
            group by c.courtType.code
            order by count(r.id) desc, c.courtType.code asc
            """)
    List<BookingCountProjection> findTopSportsByBookings();
}
