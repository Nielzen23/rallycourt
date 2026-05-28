package com.rallycourt.court.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rallycourt.auth.entity.CourtOwnerStatus;
import com.rallycourt.auth.entity.Role;
import com.rallycourt.auth.entity.User;
import com.rallycourt.court.dto.CourtManagementDetailsResponse;
import com.rallycourt.court.dto.CourtManagementRequest;
import com.rallycourt.court.dto.CourtManagementReservationSummary;
import com.rallycourt.court.dto.CourtPageResponse;
import com.rallycourt.court.entity.Court;
import com.rallycourt.court.entity.CourtStatus;
import com.rallycourt.court.entity.CourtType;
import com.rallycourt.court.entity.VenueType;
import com.rallycourt.court.exception.CourtNotFoundException;
import com.rallycourt.court.exception.CourtValidationException;
import com.rallycourt.court.geocoding.Coordinates;
import com.rallycourt.court.geocoding.GeocodingService;
import com.rallycourt.court.repository.CourtRepository;
import com.rallycourt.court.repository.CourtTypeRepository;
import com.rallycourt.court.repository.CourtTypeVenueTypeRepository;
import com.rallycourt.court.repository.VenueTypeRepository;
import com.rallycourt.payment.entity.Payment;
import com.rallycourt.payment.entity.PaymentStatus;
import com.rallycourt.payment.repository.PaymentRepository;
import com.rallycourt.reservation.entity.Reservation;
import com.rallycourt.reservation.entity.ReservationStatus;
import com.rallycourt.reservation.repository.ReservationRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class CourtManagementServiceTest {

    @Mock
    private CourtRepository courtRepository;
    @Mock
    private CourtTypeRepository courtTypeRepository;
    @Mock
    private VenueTypeRepository venueTypeRepository;
    @Mock
    private CourtTypeVenueTypeRepository courtTypeVenueTypeRepository;
    @Mock
    private CourtAccessService courtAccessService;
    @Mock
    private GeocodingService geocodingService;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private CourtManagementServiceImpl courtManagementService;

    private CourtType badminton;
    private CourtType futsal;
    private VenueType indoor;
    private VenueType outdoor;
    private User owner;

    @BeforeEach
    void setUp() {
        badminton = courtType("BADMINTON");
        futsal = courtType("FUTSAL");
        indoor = venueType("INDOOR");
        outdoor = venueType("OUTDOOR");

        Role role = new Role();
        role.setCode("COURT_OWNER");
        owner = new User();
        owner.setId(7L);
        owner.setEmail("owner@rallycourt.local");
        owner.setFirstName("Court");
        owner.setLastName("Owner");
        owner.setCourtOwnerStatus(CourtOwnerStatus.APPROVED);
        owner.setRole(role);
    }

    @Test
    void getCourtsReturnsFilteredPage() {
        Court court = new Court();
        court.setName("Managed Court");

        when(courtTypeRepository.findByCode("BADMINTON")).thenReturn(Optional.of(badminton));
        when(venueTypeRepository.findByCode("INDOOR")).thenReturn(Optional.of(indoor));
        when(courtRepository.findAllFiltered("BADMINTON", "INDOOR", CourtStatus.AVAILABLE, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(court), PageRequest.of(0, 10), 1));

        CourtPageResponse response = courtManagementService.getCourts("badminton", "indoor", "available", 0, 10);

        assertEquals(1, response.totalElements());
        assertEquals("Managed Court", response.content().getFirst().getName());
    }

    @Test
    void getCourtsReturnsUnfilteredPageWhenFiltersAreBlank() {
        Court court = new Court();
        court.setName("Managed Court");

        when(courtRepository.findAllFiltered(null, null, null, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(court), PageRequest.of(0, 10), 1));

        CourtPageResponse response = courtManagementService.getCourts(" ", " ", " ", 0, 10);

        assertEquals(1, response.totalElements());
        assertEquals("Managed Court", response.content().getFirst().getName());
    }

    @Test
    void getAddressSuggestionsDelegatesTrimmedQuery() {
        when(geocodingService.autocomplete("Makati")).thenReturn(List.of());

        List<?> response = courtManagementService.getAddressSuggestions(" Makati ");

        assertSame(List.of(), response);
        verify(geocodingService).autocomplete("Makati");
    }

    @Test
    void getAddressSuggestionsUsesEmptyQueryWhenNull() {
        when(geocodingService.autocomplete("")).thenReturn(List.of());

        List<?> response = courtManagementService.getAddressSuggestions(null);

        assertSame(List.of(), response);
        verify(geocodingService).autocomplete("");
    }

    @Test
    void createCourtUsesDefaultsAndPersistsCoordinates() {
        CourtManagementRequest request = request("Managed Court", "Makati City", "BADMINTON", "INDOOR", "AVAILABLE");
        request.setOpenTime(null);
        request.setCloseTime(null);

        when(courtTypeRepository.findByCode("BADMINTON")).thenReturn(Optional.of(badminton));
        when(venueTypeRepository.findByCode("INDOOR")).thenReturn(Optional.of(indoor));
        when(courtTypeVenueTypeRepository.existsByCourtType_CodeAndVenueType_Code("BADMINTON", "INDOOR")).thenReturn(true);
        when(geocodingService.geocode("Makati City")).thenReturn(new Coordinates(14.5547, 121.0244));
        when(courtAccessService.getCurrentUser()).thenReturn(owner);
        when(courtRepository.save(any(Court.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Court saved = courtManagementService.createCourt(request);

        assertEquals(LocalTime.of(8, 0), saved.getOpenTime());
        assertEquals(LocalTime.of(22, 0), saved.getCloseTime());
        assertEquals(owner, saved.getOwner());
        assertEquals(14.5547, saved.getLatitude());
        assertEquals(121.0244, saved.getLongitude());
        assertEquals(BigDecimal.valueOf(750), saved.getHourlyRate());
    }

    @Test
    void createCourtRejectsUnsupportedVenueCombinationBeforeGeocoding() {
        CourtManagementRequest request = request("Managed Court", "Makati City", "FUTSAL", "OUTDOOR", "AVAILABLE");

        when(courtTypeRepository.findByCode("FUTSAL")).thenReturn(Optional.of(futsal));
        when(venueTypeRepository.findByCode("OUTDOOR")).thenReturn(Optional.of(outdoor));
        when(courtTypeVenueTypeRepository.existsByCourtType_CodeAndVenueType_Code("FUTSAL", "OUTDOOR")).thenReturn(false);

        CourtValidationException exception = assertThrows(
                CourtValidationException.class,
                () -> courtManagementService.createCourt(request)
        );

        assertEquals("FUTSAL is not supported for OUTDOOR venues", exception.getMessage());
        verify(geocodingService, never()).geocode(any());
    }

    @Test
    void createCourtRejectsUnsupportedCourtType() {
        CourtManagementRequest request = request("Managed Court", "Makati City", "TENNIS", "INDOOR", "AVAILABLE");
        when(courtTypeRepository.findByCode("TENNIS")).thenReturn(Optional.empty());

        CourtValidationException exception = assertThrows(
                CourtValidationException.class,
                () -> courtManagementService.createCourt(request)
        );

        assertEquals("Unsupported court type: TENNIS", exception.getMessage());
    }

    @Test
    void createCourtRejectsUnsupportedVenueType() {
        CourtManagementRequest request = request("Managed Court", "Makati City", "BADMINTON", "ROOFED", "AVAILABLE");
        when(courtTypeRepository.findByCode("BADMINTON")).thenReturn(Optional.of(badminton));
        when(venueTypeRepository.findByCode("ROOFED")).thenReturn(Optional.empty());

        CourtValidationException exception = assertThrows(
                CourtValidationException.class,
                () -> courtManagementService.createCourt(request)
        );

        assertEquals("Unsupported venue type: ROOFED", exception.getMessage());
    }

    @Test
    void createCourtRejectsMissingStatus() {
        CourtManagementRequest request = request("Managed Court", "Makati City", "BADMINTON", "INDOOR", null);
        when(courtTypeRepository.findByCode("BADMINTON")).thenReturn(Optional.of(badminton));
        when(venueTypeRepository.findByCode("INDOOR")).thenReturn(Optional.of(indoor));

        CourtValidationException exception = assertThrows(
                CourtValidationException.class,
                () -> courtManagementService.createCourt(request)
        );

        assertEquals("Court status is required", exception.getMessage());
    }

    @Test
    void createCourtRejectsUnsupportedStatus() {
        CourtManagementRequest request = request("Managed Court", "Makati City", "BADMINTON", "INDOOR", "BROKEN");
        when(courtTypeRepository.findByCode("BADMINTON")).thenReturn(Optional.of(badminton));
        when(venueTypeRepository.findByCode("INDOOR")).thenReturn(Optional.of(indoor));

        CourtValidationException exception = assertThrows(
                CourtValidationException.class,
                () -> courtManagementService.createCourt(request)
        );

        assertEquals("Unsupported court status: BROKEN", exception.getMessage());
    }

    @Test
    void createCourtRejectsInvalidOperatingHours() {
        CourtManagementRequest request = request("Managed Court", "Makati City", "BADMINTON", "INDOOR", "AVAILABLE");
        request.setOpenTime(LocalTime.of(22, 0));
        request.setCloseTime(LocalTime.of(8, 0));

        when(courtTypeRepository.findByCode("BADMINTON")).thenReturn(Optional.of(badminton));
        when(venueTypeRepository.findByCode("INDOOR")).thenReturn(Optional.of(indoor));
        when(courtTypeVenueTypeRepository.existsByCourtType_CodeAndVenueType_Code("BADMINTON", "INDOOR")).thenReturn(true);

        CourtValidationException exception = assertThrows(
                CourtValidationException.class,
                () -> courtManagementService.createCourt(request)
        );

        assertEquals("Open time must be earlier than close time", exception.getMessage());
    }

    @Test
    void updateCourtGeocodesWhenLocationChanges() {
        Court court = existingCourt();
        CourtManagementRequest request = request("Updated Court", "Ortigas Center", "BADMINTON", "INDOOR", "UNAVAILABLE");
        request.setOpenTime(LocalTime.of(9, 0));
        request.setCloseTime(LocalTime.of(21, 0));

        when(courtRepository.findById(10L)).thenReturn(Optional.of(court));
        when(courtTypeRepository.findByCode("BADMINTON")).thenReturn(Optional.of(badminton));
        when(venueTypeRepository.findByCode("INDOOR")).thenReturn(Optional.of(indoor));
        when(courtTypeVenueTypeRepository.existsByCourtType_CodeAndVenueType_Code("BADMINTON", "INDOOR")).thenReturn(true);
        when(geocodingService.geocode("Ortigas Center")).thenReturn(new Coordinates(14.5869, 121.0614));
        when(courtRepository.save(court)).thenReturn(court);

        Court updated = courtManagementService.updateCourt(10L, request);

        assertEquals("Updated Court", updated.getName());
        assertEquals("Ortigas Center", updated.getLocation());
        assertEquals(CourtStatus.UNAVAILABLE, updated.getStatus());
        assertEquals(14.5869, updated.getLatitude());
        assertEquals(121.0614, updated.getLongitude());
    }

    @Test
    void updateCourtKeepsCoordinatesWhenLocationDoesNotChange() {
        Court court = existingCourt();
        CourtManagementRequest request = request("Updated Court", "Makati City", "BADMINTON", "INDOOR", "AVAILABLE");

        when(courtRepository.findById(10L)).thenReturn(Optional.of(court));
        when(courtTypeRepository.findByCode("BADMINTON")).thenReturn(Optional.of(badminton));
        when(venueTypeRepository.findByCode("INDOOR")).thenReturn(Optional.of(indoor));
        when(courtTypeVenueTypeRepository.existsByCourtType_CodeAndVenueType_Code("BADMINTON", "INDOOR")).thenReturn(true);
        when(courtRepository.save(court)).thenReturn(court);

        Court updated = courtManagementService.updateCourt(10L, request);

        assertEquals(14.5547, updated.getLatitude());
        verify(geocodingService, never()).geocode(any());
    }

    @Test
    void updateCourtUsesExistingTimesWhenRequestOmitsThem() {
        Court court = existingCourt();
        CourtManagementRequest request = request("Updated Court", "Makati City", "BADMINTON", "INDOOR", "AVAILABLE");
        request.setOpenTime(null);
        request.setCloseTime(null);

        when(courtRepository.findById(10L)).thenReturn(Optional.of(court));
        when(courtTypeRepository.findByCode("BADMINTON")).thenReturn(Optional.of(badminton));
        when(venueTypeRepository.findByCode("INDOOR")).thenReturn(Optional.of(indoor));
        when(courtTypeVenueTypeRepository.existsByCourtType_CodeAndVenueType_Code("BADMINTON", "INDOOR")).thenReturn(true);
        when(courtRepository.save(court)).thenReturn(court);

        Court updated = courtManagementService.updateCourt(10L, request);

        assertEquals(LocalTime.of(8, 0), updated.getOpenTime());
        assertEquals(LocalTime.of(22, 0), updated.getCloseTime());
    }

    @Test
    void updateCourtRejectsMissingCourt() {
        CourtManagementRequest request = request("Updated Court", "Makati City", "BADMINTON", "INDOOR", "AVAILABLE");
        when(courtRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(CourtNotFoundException.class, () -> courtManagementService.updateCourt(10L, request));
    }

    @Test
    void deleteCourtRejectsActiveFutureReservations() {
        Court court = existingCourt();
        when(courtRepository.findById(10L)).thenReturn(Optional.of(court));
        when(reservationRepository.existsByCourtIdAndStatusInAndStartTimeGreaterThan(eq(10L), any(), any())).thenReturn(true);

        CourtValidationException exception = assertThrows(
                CourtValidationException.class,
                () -> courtManagementService.deleteCourt(10L)
        );

        assertEquals("Court cannot be deleted while it has active future reservations", exception.getMessage());
    }

    @Test
    void deleteCourtDeletesWhenNoActiveFutureReservationsExist() {
        Court court = existingCourt();
        when(courtRepository.findById(10L)).thenReturn(Optional.of(court));
        when(reservationRepository.existsByCourtIdAndStatusInAndStartTimeGreaterThan(eq(10L), any(), any())).thenReturn(false);

        courtManagementService.deleteCourt(10L);

        verify(courtRepository).delete(court);
    }

    @Test
    void deleteCourtRejectsMissingCourt() {
        when(courtRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(CourtNotFoundException.class, () -> courtManagementService.deleteCourt(10L));
    }

    @Test
    void getCourtDetailsIncludesUpcomingReservations() {
        Court court = existingCourt();
        court.setHourlyRate(BigDecimal.valueOf(800));

        Reservation reservation = new Reservation();
        reservation.setId(20L);
        reservation.setCourtId(10L);
        reservation.setReservedBy("player@rallycourt.local");
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setStartTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
        reservation.setEndTime(reservation.getStartTime().plusMinutes(90));

        Payment payment = new Payment();
        payment.setReservationId(20L);
        payment.setStatus(PaymentStatus.SUCCESS);

        when(courtRepository.findById(10L)).thenReturn(Optional.of(court));
        when(courtRepository.existsById(10L)).thenReturn(true);
        when(reservationRepository.findByCourtIdAndStartTimeGreaterThanEqualOrderByStartTimeAsc(eq(10L), any()))
                .thenReturn(List.of(reservation));
        when(paymentRepository.findByReservationIdIn(List.of(20L))).thenReturn(List.of(payment));

        CourtManagementDetailsResponse response = courtManagementService.getCourtDetails(10L);

        assertEquals("Managed Court", response.name());
        assertEquals(BigDecimal.valueOf(800), response.hourlyRate());
        assertEquals(1, response.upcomingReservations().size());
        CourtManagementReservationSummary summary = response.upcomingReservations().getFirst();
        assertEquals("PAID", summary.paymentStatus());
        assertEquals("CONFIRMED", summary.reservationStatus());
        assertEquals(90, summary.durationMinutes());
    }

    @Test
    void getCourtDetailsRejectsMissingCourt() {
        when(courtRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(CourtNotFoundException.class, () -> courtManagementService.getCourtDetails(10L));
    }

    @Test
    void getUpcomingReservationsRejectsMissingCourt() {
        when(courtRepository.existsById(99L)).thenReturn(false);

        assertThrows(CourtNotFoundException.class, () -> courtManagementService.getUpcomingReservations(99L));
    }

    @Test
    void getUpcomingReservationsMarksMissingSuccessfulPaymentAsPending() {
        Reservation reservation = new Reservation();
        reservation.setId(20L);
        reservation.setReservedBy("player@rallycourt.local");
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setStartTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
        reservation.setEndTime(reservation.getStartTime().plusMinutes(60));

        when(courtRepository.existsById(10L)).thenReturn(true);
        when(reservationRepository.findByCourtIdAndStartTimeGreaterThanEqualOrderByStartTimeAsc(eq(10L), any()))
                .thenReturn(List.of(reservation));
        when(paymentRepository.findByReservationIdIn(List.of(20L))).thenReturn(List.of());

        List<CourtManagementReservationSummary> summaries = courtManagementService.getUpcomingReservations(10L);

        assertEquals("PENDING", summaries.getFirst().paymentStatus());
    }

    @Test
    void getUpcomingReservationsUsesLastPaymentWhenDuplicatePaymentsExist() {
        Reservation reservation = new Reservation();
        reservation.setId(20L);
        reservation.setReservedBy("player@rallycourt.local");
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setStartTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
        reservation.setEndTime(reservation.getStartTime().plusMinutes(60));

        Payment firstPayment = new Payment();
        firstPayment.setReservationId(20L);
        firstPayment.setStatus(PaymentStatus.SUCCESS);

        Payment lastPayment = new Payment();
        lastPayment.setReservationId(20L);
        lastPayment.setStatus(PaymentStatus.FAILED);

        when(courtRepository.existsById(10L)).thenReturn(true);
        when(reservationRepository.findByCourtIdAndStartTimeGreaterThanEqualOrderByStartTimeAsc(eq(10L), any()))
                .thenReturn(List.of(reservation));
        when(paymentRepository.findByReservationIdIn(List.of(20L))).thenReturn(List.of(firstPayment, lastPayment));

        List<CourtManagementReservationSummary> summaries = courtManagementService.getUpcomingReservations(10L);

        assertEquals("PENDING", summaries.getFirst().paymentStatus());
    }

    private CourtManagementRequest request(
            String name,
            String location,
            String courtType,
            String venueType,
            String status
    ) {
        CourtManagementRequest request = new CourtManagementRequest();
        request.setName(name);
        request.setLocation(location);
        request.setCourtType(courtType);
        request.setVenueType(venueType);
        request.setStatus(status);
        request.setOpenTime(LocalTime.of(8, 0));
        request.setCloseTime(LocalTime.of(22, 0));
        request.setHourlyRate(BigDecimal.valueOf(750));
        return request;
    }

    private Court existingCourt() {
        Court court = new Court();
        court.setId(10L);
        court.setName("Managed Court");
        court.setLocation("Makati City");
        court.setLatitude(14.5547);
        court.setLongitude(121.0244);
        court.setCourtType(badminton);
        court.setVenueType(indoor);
        court.setStatus(CourtStatus.AVAILABLE);
        court.setOpenTime(LocalTime.of(8, 0));
        court.setCloseTime(LocalTime.of(22, 0));
        court.setHourlyRate(BigDecimal.valueOf(750));
        court.setOwner(owner);
        return court;
    }

    private CourtType courtType(String code) {
        CourtType courtType = new CourtType();
        courtType.setCode(code);
        return courtType;
    }

    private VenueType venueType(String code) {
        VenueType venueType = new VenueType();
        venueType.setCode(code);
        return venueType;
    }
}
