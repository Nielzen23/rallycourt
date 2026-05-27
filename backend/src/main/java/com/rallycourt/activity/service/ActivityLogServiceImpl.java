package com.rallycourt.activity.service;

import com.rallycourt.activity.dto.AdminActivityHistoryItemResponse;
import com.rallycourt.activity.dto.AdminActivityHistoryPageResponse;
import com.rallycourt.activity.dto.AdminUserActivitySummaryResponse;
import com.rallycourt.activity.entity.ActivityLog;
import com.rallycourt.activity.exception.ActivityUserNotFoundException;
import com.rallycourt.activity.repository.ActivityLogRepository;
import com.rallycourt.auth.entity.User;
import com.rallycourt.auth.repository.UserRepository;
import com.rallycourt.payment.entity.Payment;
import com.rallycourt.payment.entity.PaymentStatus;
import com.rallycourt.payment.repository.PaymentRepository;
import com.rallycourt.reservation.entity.Reservation;
import com.rallycourt.reservation.repository.ReservationRepository;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityLogServiceImpl implements ActivityLogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityLogServiceImpl.class);
    private static final String SYSTEM_ACTOR = "system";

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;

    public ActivityLogServiceImpl(
            ActivityLogRepository activityLogRepository,
            UserRepository userRepository,
            ReservationRepository reservationRepository,
            PaymentRepository paymentRepository
    ) {
        this.activityLogRepository = activityLogRepository;
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
        this.paymentRepository = paymentRepository;
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

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserActivitySummaryResponse> getUserActivitySummaries() {
        List<User> users = userRepository.findAll();
        Map<String, ActivityLog> latestActivityByActor = activityLogRepository.findByActorIn(
                        users.stream().map(user -> String.valueOf(user.getId())).toList()
                ).stream()
                .collect(Collectors.toMap(
                        ActivityLog::getActor,
                        Function.identity(),
                        (left, right) -> left.getCreatedAt().isAfter(right.getCreatedAt()) ? left : right
                ));

        Map<String, BookingStats> bookingStatsByEmail = buildBookingStatsByEmail(
                reservationRepository.findAll()
        );

        return users.stream()
                .sorted(Comparator.comparing(User::getEmail, String.CASE_INSENSITIVE_ORDER))
                .map(user -> {
                    ActivityLog lastActivity = latestActivityByActor.get(String.valueOf(user.getId()));
                    BookingStats bookingStats = bookingStatsByEmail.getOrDefault(
                            user.getEmail(),
                            BookingStats.empty()
                    );

                    return new AdminUserActivitySummaryResponse(
                            user.getId(),
                            user.getEmail(),
                            user.getFirstName(),
                            user.getLastName(),
                            user.getRole().getCode(),
                            lastActivity != null ? lastActivity.getCreatedAt() : null,
                            lastActivity != null ? lastActivity.getAction() : null,
                            bookingStats.successfulBookings(),
                            bookingStats.failedBookings(),
                            bookingStats.totalBookings(),
                            bookingStats.successRatioPercentage()
                    );
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminActivityHistoryPageResponse getUserActivityHistory(Long userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ActivityUserNotFoundException(userId));

        Page<ActivityLog> activityPage = activityLogRepository.findByActorOrderByCreatedAtDesc(
                String.valueOf(user.getId()),
                PageRequest.of(page, size)
        );

        return new AdminActivityHistoryPageResponse(
                activityPage.getContent().stream()
                        .map(item -> new AdminActivityHistoryItemResponse(
                                item.getId(),
                                item.getAction(),
                                item.getStatus(),
                                item.getCreatedAt()
                        ))
                        .toList(),
                activityPage.getNumber(),
                activityPage.getSize(),
                activityPage.getTotalElements(),
                activityPage.getTotalPages()
        );
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

    private Map<String, BookingStats> buildBookingStatsByEmail(Collection<Reservation> reservations) {
        Map<Long, Payment> paymentsByReservationId = paymentRepository.findByReservationIdIn(
                        reservations.stream().map(Reservation::getId).toList()
                ).stream()
                .collect(Collectors.toMap(Payment::getReservationId, Function.identity(), (left, right) -> right));

        return reservations.stream()
                .collect(Collectors.groupingBy(
                        Reservation::getReservedBy,
                        Collectors.collectingAndThen(Collectors.toList(), userReservations -> {
                            int successfulBookings = (int) userReservations.stream()
                                    .filter(reservation -> {
                                        Payment payment = paymentsByReservationId.get(reservation.getId());
                                        return payment != null && payment.getStatus() == PaymentStatus.SUCCESS;
                                    })
                                    .count();
                            int totalBookings = userReservations.size();
                            return BookingStats.fromCounts(successfulBookings, totalBookings - successfulBookings);
                        })
                ));
    }

    private record BookingStats(
            int successfulBookings,
            int failedBookings,
            int totalBookings,
            int successRatioPercentage
    ) {
        private static BookingStats fromCounts(int successfulBookings, int failedBookings) {
            int totalBookings = successfulBookings + failedBookings;
            int successRatioPercentage = totalBookings == 0
                    ? 0
                    : Math.toIntExact(Math.round((successfulBookings * 100.0) / totalBookings));
            return new BookingStats(successfulBookings, failedBookings, totalBookings, successRatioPercentage);
        }

        private static BookingStats empty() {
            return fromCounts(0, 0);
        }
    }
}
