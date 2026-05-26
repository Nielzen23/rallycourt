package com.rallycourt.court.repository;

import com.rallycourt.court.entity.CourtTypeVenueType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourtTypeVenueTypeRepository extends JpaRepository<CourtTypeVenueType, Long> {

    boolean existsByCourtType_CodeAndVenueType_Code(String courtTypeCode, String venueTypeCode);
}
