package com.rallycourt.dashboard.service;

import com.rallycourt.activity.repository.ActivityLogRepository;
import com.rallycourt.auth.entity.User;
import com.rallycourt.auth.repository.UserRepository;
import com.rallycourt.dashboard.dto.DashboardCountItemResponse;
import com.rallycourt.dashboard.dto.DashboardResponse;
import com.rallycourt.dashboard.dto.DashboardUserSummaryResponse;
import com.rallycourt.payment.entity.PaymentStatus;
import com.rallycourt.payment.repository.PaymentRepository;
import com.rallycourt.reservation.repository.ReservationRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardServiceImpl.class);
    private static final int TOP_ITEMS_LIMIT = 5;

    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final ActivityLogRepository activityLogRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        LOGGER.info("Fetching dashboard analytics");

        long totalBookings = reservationRepository.count();
        long paidBookings = paymentRepository.countDistinctReservationIdByStatus(PaymentStatus.SUCCESS);
        long unpaidBookings = Math.max(0, totalBookings - paidBookings);

        List<DashboardCountItemResponse> paymentBreakdown = List.of(
                new DashboardCountItemResponse("Paid", paidBookings),
                new DashboardCountItemResponse("Unpaid", unpaidBookings)
        );

        List<DashboardCountItemResponse> popularCourts = reservationRepository.findTopCourtsByBookings(
                        PageRequest.of(0, TOP_ITEMS_LIMIT)
                ).stream()
                .map(item -> new DashboardCountItemResponse(item.getLabel(), item.getCount()))
                .toList();

        List<DashboardCountItemResponse> popularSports = reservationRepository.findTopSportsByBookings().stream()
                .map(item -> new DashboardCountItemResponse(item.getLabel(), item.getCount()))
                .toList();

        DashboardUserSummaryResponse userSummary = buildUserSummary();

        return new DashboardResponse(
                totalBookings,
                paidBookings,
                unpaidBookings,
                paymentBreakdown,
                popularCourts,
                popularSports,
                userSummary
        );
    }

    private DashboardUserSummaryResponse buildUserSummary() {
        List<User> users = userRepository.findAll();
        Set<String> usersWithBookings = new HashSet<>(reservationRepository.findDistinctReservedBy());
        Set<String> usersWithActivity = activityLogRepository.findAll().stream()
                .map(activityLog -> activityLog.getActor())
                .filter(actor -> actor != null && !actor.isBlank())
                .collect(HashSet::new, Set::add, Set::addAll);

        long usersWithBookingsCount = users.stream()
                .map(User::getEmail)
                .filter(usersWithBookings::contains)
                .count();

        long activeUsersWithoutBookingsCount = users.stream()
                .map(User::getEmail)
                .filter(usersWithActivity::contains)
                .filter(email -> !usersWithBookings.contains(email))
                .count();

        long inactiveUsersCount = users.stream()
                .map(User::getEmail)
                .filter(email -> !usersWithActivity.contains(email))
                .count();

        LOGGER.debug(
                "Dashboard user summary calculated: withBookings={}, activeWithoutBookings={}, inactive={}",
                usersWithBookingsCount,
                activeUsersWithoutBookingsCount,
                inactiveUsersCount
        );

        return new DashboardUserSummaryResponse(
                usersWithBookingsCount,
                activeUsersWithoutBookingsCount,
                inactiveUsersCount
        );
    }
}
