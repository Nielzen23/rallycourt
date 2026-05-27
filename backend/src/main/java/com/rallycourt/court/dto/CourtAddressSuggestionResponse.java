package com.rallycourt.court.dto;

public record CourtAddressSuggestionResponse(
        String address,
        Double latitude,
        Double longitude
) {
}
