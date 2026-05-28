package com.rallycourt.dashboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.rallycourt.activity.entity.ActivityLog;
import com.rallycourt.activity.repository.ActivityLogRepository;
import com.rallycourt.auth.entity.Role;
import com.rallycourt.auth.entity.User;
import com.rallycourt.auth.repository.UserRepository;
import com.rallycourt.dashboard.dto.DashboardResponse;
import com.rallycourt.payment.entity.PaymentStatus;
import com.rallycourt.payment.repository.PaymentRepository;
import com.rallycourt.reservation.repository.ReservationRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ActivityLogRepository activityLogRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @Test
    void getDashboardBuildsCountsAndUserSummary() {
        when(reservationRepository.count()).thenReturn(3L);
        when(paymentRepository.countDistinctReservationIdByStatus(PaymentStatus.SUCCESS)).thenReturn(1L);
        when(reservationRepository.findTopCourtsByBookings(org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(List.of(new LabelCountProjection("Court A", 2L)));
        when(reservationRepository.findTopSportsByBookings())
                .thenReturn(List.of(new LabelCountProjection("BADMINTON", 3L)));

        User bookedUser = user("booked@rallycourt.local");
        User activeOnlyUser = user("active@rallycourt.local");
        User inactiveUser = user("inactive@rallycourt.local");
        when(userRepository.findAll()).thenReturn(List.of(bookedUser, activeOnlyUser, inactiveUser));
        when(reservationRepository.findDistinctReservedBy()).thenReturn(List.of("booked@rallycourt.local"));

        ActivityLog bookedActivity = new ActivityLog();
        bookedActivity.setActor("booked@rallycourt.local");
        ActivityLog activeOnlyActivity = new ActivityLog();
        activeOnlyActivity.setActor("active@rallycourt.local");
        ActivityLog blankActivity = new ActivityLog();
        blankActivity.setActor(" ");
        when(activityLogRepository.findAll()).thenReturn(List.of(bookedActivity, activeOnlyActivity, blankActivity));

        DashboardResponse response = dashboardService.getDashboard();

        assertEquals(3L, response.totalBookings());
        assertEquals(1L, response.paidBookings());
        assertEquals(2L, response.unpaidBookings());
        assertEquals(2, response.bookingPaymentBreakdown().size());
        assertEquals("Court A", response.popularCourts().getFirst().label());
        assertEquals("BADMINTON", response.popularSports().getFirst().label());
        assertEquals(1L, response.userSummary().usersWithBookings());
        assertEquals(1L, response.userSummary().activeUsersWithoutBookings());
        assertEquals(1L, response.userSummary().inactiveUsers());
    }

    @Test
    void getDashboardClampsUnpaidBookingsAtZero() {
        when(reservationRepository.count()).thenReturn(1L);
        when(paymentRepository.countDistinctReservationIdByStatus(PaymentStatus.SUCCESS)).thenReturn(5L);
        when(reservationRepository.findTopCourtsByBookings(org.mockito.ArgumentMatchers.any(Pageable.class))).thenReturn(List.of());
        when(reservationRepository.findTopSportsByBookings()).thenReturn(List.of());
        when(userRepository.findAll()).thenReturn(List.of());
        when(reservationRepository.findDistinctReservedBy()).thenReturn(List.of());
        when(activityLogRepository.findAll()).thenReturn(List.of());

        DashboardResponse response = dashboardService.getDashboard();

        assertEquals(0L, response.unpaidBookings());
        assertEquals(0L, response.userSummary().usersWithBookings());
        assertEquals(0L, response.userSummary().activeUsersWithoutBookings());
        assertEquals(0L, response.userSummary().inactiveUsers());
    }

    @Test
    void getDashboardIgnoresNullActivityActors() {
        when(reservationRepository.count()).thenReturn(0L);
        when(paymentRepository.countDistinctReservationIdByStatus(PaymentStatus.SUCCESS)).thenReturn(0L);
        when(reservationRepository.findTopCourtsByBookings(org.mockito.ArgumentMatchers.any(Pageable.class))).thenReturn(List.of());
        when(reservationRepository.findTopSportsByBookings()).thenReturn(List.of());

        User user = user("active@rallycourt.local");
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(reservationRepository.findDistinctReservedBy()).thenReturn(List.of());

        ActivityLog nullActorActivity = new ActivityLog();
        nullActorActivity.setActor(null);
        when(activityLogRepository.findAll()).thenReturn(List.of(nullActorActivity));

        DashboardResponse response = dashboardService.getDashboard();

        assertEquals(0L, response.userSummary().usersWithBookings());
        assertEquals(0L, response.userSummary().activeUsersWithoutBookings());
        assertEquals(1L, response.userSummary().inactiveUsers());
    }

    private User user(String email) {
        User user = new User();
        user.setEmail(email);
        Role role = new Role();
        role.setCode("PLAYER");
        user.setRole(role);
        return user;
    }

    private record LabelCountProjection(String label, Long count)
            implements com.rallycourt.reservation.repository.BookingCountProjection {
        @Override
        public String getLabel() {
            return label;
        }

        @Override
        public long getCount() {
            return count;
        }
    }
}
