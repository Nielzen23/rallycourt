package com.rallycourt.court.geocoding;

import com.rallycourt.court.dto.CourtAddressSuggestionResponse;
import java.util.List;

public interface GeocodingService {

    Coordinates geocode(String location);

    List<CourtAddressSuggestionResponse> autocomplete(String query);
}
