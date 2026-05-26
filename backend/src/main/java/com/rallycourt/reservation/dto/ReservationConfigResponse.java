package com.rallycourt.reservation.dto;

import java.util.List;

public record ReservationConfigResponse(List<Integer> allowedDurationsMinutes) {
}
