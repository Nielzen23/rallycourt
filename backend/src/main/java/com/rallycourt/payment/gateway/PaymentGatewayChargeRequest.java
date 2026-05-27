package com.rallycourt.payment.gateway;

import java.math.BigDecimal;

public record PaymentGatewayChargeRequest(
        Long reservationId,
        BigDecimal amount,
        String paymentMethodToken
) {
}
