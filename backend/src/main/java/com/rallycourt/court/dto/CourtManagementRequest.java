package com.rallycourt.court.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourtManagementRequest {

    @NotBlank(message = "Court name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Location is required")
    @Size(max = 255)
    private String location;

    @NotBlank(message = "Court type is required")
    private String courtType;

    @NotBlank(message = "Venue type is required")
    private String venueType;

    @NotBlank(message = "Court status is required")
    private String status;

    private LocalTime openTime;

    private LocalTime closeTime;

    @NotNull(message = "Hourly rate is required")
    @DecimalMin(value = "0.01", message = "Hourly rate must be greater than zero")
    private BigDecimal hourlyRate;
}
