package com.rallycourt.court.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rallycourt.court.dto.CourtAddressSuggestionResponse;
import com.rallycourt.court.dto.CourtManagementDetailsResponse;
import com.rallycourt.court.dto.CourtManagementRequest;
import com.rallycourt.court.dto.CourtManagementReservationSummary;
import com.rallycourt.court.dto.CourtPageResponse;
import com.rallycourt.court.entity.Court;
import com.rallycourt.court.service.CourtManagementService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class CourtManagementControllerTest {

    @Mock
    private CourtManagementService courtManagementService;

    @InjectMocks
    private CourtManagementController controller;

    @Test
    void getCourtsReturnsServiceResponse() {
        CourtPageResponse expected = new CourtPageResponse(List.of(), 0, 10, 0, 0);
        when(courtManagementService.getCourts("BADMINTON", "INDOOR", "AVAILABLE", 0, 10)).thenReturn(expected);

        var response = controller.getCourts("BADMINTON", "INDOOR", "AVAILABLE", 0, 10);

        assertEquals(expected, response.getBody());
    }

    @Test
    void getAddressSuggestionsReturnsServiceResponse() {
        List<CourtAddressSuggestionResponse> expected = List.of(new CourtAddressSuggestionResponse("Makati", 1.0, 2.0));
        when(courtManagementService.getAddressSuggestions("Makati")).thenReturn(expected);

        var response = controller.getAddressSuggestions("Makati");

        assertEquals(expected, response.getBody());
    }

    @Test
    void getAddressSuggestionsHandlesNullQueryForLoggingBranch() {
        List<CourtAddressSuggestionResponse> expected = List.of();
        when(courtManagementService.getAddressSuggestions(null)).thenReturn(expected);

        var response = controller.getAddressSuggestions(null);

        assertEquals(expected, response.getBody());
    }

    @Test
    void createCourtReturnsCreatedResponse() {
        Court court = new Court();
        CourtManagementRequest request = new CourtManagementRequest();
        when(courtManagementService.createCourt(request)).thenReturn(court);

        var response = controller.createCourt(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(court, response.getBody());
    }

    @Test
    void updateCourtReturnsServiceResponse() {
        Court court = new Court();
        CourtManagementRequest request = new CourtManagementRequest();
        when(courtManagementService.updateCourt(10L, request)).thenReturn(court);

        var response = controller.updateCourt(10L, request);

        assertEquals(court, response.getBody());
    }

    @Test
    void deleteCourtReturnsNoContent() {
        var response = controller.deleteCourt(10L);

        verify(courtManagementService).deleteCourt(10L);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void getCourtDetailsReturnsServiceResponse() {
        CourtManagementDetailsResponse details = new CourtManagementDetailsResponse(
                10L, "Court", "BADMINTON", "INDOOR", "Makati", 1.0, 2.0,
                "AVAILABLE", "08:00", "22:00", BigDecimal.TEN, List.of()
        );
        when(courtManagementService.getCourtDetails(10L)).thenReturn(details);

        var response = controller.getCourtDetails(10L);

        assertEquals(details, response.getBody());
    }

    @Test
    void getCourtReservationsReturnsServiceResponse() {
        List<CourtManagementReservationSummary> reservations = List.of(
                new CourtManagementReservationSummary(1L, LocalDate.now(), LocalTime.NOON, 60, "PAID", "CONFIRMED", "player")
        );
        when(courtManagementService.getUpcomingReservations(10L)).thenReturn(reservations);

        var response = controller.getCourtReservations(10L);

        assertEquals(reservations, response.getBody());
    }
}
