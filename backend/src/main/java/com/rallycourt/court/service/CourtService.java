package com.rallycourt.court.service;

import com.rallycourt.court.dto.CourtPageResponse;
import com.rallycourt.court.dto.CourtRequest;
import com.rallycourt.court.entity.Court;

public interface CourtService {

    CourtPageResponse getCourts(String courtTypeCode, String venueTypeCode, String statusCode, int page, int size);

    Court createCourt(CourtRequest request);

    Court updateCourt(Long courtId, CourtRequest request);
}
