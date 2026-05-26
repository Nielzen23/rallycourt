package com.rallycourt.court.exception;

public class CourtNotFoundException extends RuntimeException {

    public CourtNotFoundException(Long courtId) {
        super("Court not found: " + courtId);
    }
}
