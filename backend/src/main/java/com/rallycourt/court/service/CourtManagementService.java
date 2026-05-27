package com.rallycourt.court.service;

import com.rallycourt.court.dto.CourtAddressSuggestionResponse;
import com.rallycourt.court.dto.CourtManagementDetailsResponse;
import com.rallycourt.court.dto.CourtManagementRequest;
import com.rallycourt.court.dto.CourtManagementReservationSummary;
import com.rallycourt.court.dto.CourtPageResponse;
import com.rallycourt.court.entity.Court;
import java.util.List;

public interface CourtManagementService {

    CourtPageResponse getCourts(String courtTypeCode, String venueTypeCode, String statusCode, int page, int size);

    List<CourtAddressSuggestionResponse> getAddressSuggestions(String query);

    Court createCourt(CourtManagementRequest request);

    Court updateCourt(Long courtId, CourtManagementRequest request);

    void deleteCourt(Long courtId);

    CourtManagementDetailsResponse getCourtDetails(Long courtId);

    List<CourtManagementReservationSummary> getUpcomingReservations(Long courtId);
}
