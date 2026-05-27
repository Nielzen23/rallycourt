package com.rallycourt.reservation.service;

import com.rallycourt.reservation.dto.CourtAvailabilityResponse;
import com.rallycourt.reservation.dto.CreateReservationRequest;
import com.rallycourt.reservation.dto.ReservationConfigResponse;
import com.rallycourt.reservation.dto.ReservationPageResponse;
import com.rallycourt.reservation.entity.Reservation;
import java.time.LocalDateTime;
import java.util.List;

public interface ReservationService {

    Reservation createReservation(CreateReservationRequest request);

    List<CourtAvailabilityResponse> getCourtAvailability(Long courtId, LocalDateTime from, LocalDateTime to);

    ReservationPageResponse getMyReservations(int page, int size);

    ReservationPageResponse getAllReservations(int page, int size);

    ReservationConfigResponse getReservationConfig();

    Reservation cancelReservation(Long reservationId);

    void autoCancelExpiredReservations();
}
