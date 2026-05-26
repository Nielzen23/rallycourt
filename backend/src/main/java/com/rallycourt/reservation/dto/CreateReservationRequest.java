package com.rallycourt.reservation.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReservationRequest {

    @NotNull
    private Long courtId;

    @NotNull
    @Future
    private LocalDateTime startTime;

    @NotNull
    private Integer durationMinutes;
}
