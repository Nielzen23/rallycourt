package com.rallycourt.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProcessPaymentRequest {

    @NotNull
    private Long reservationId;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    private String paymentMethodToken;
}
