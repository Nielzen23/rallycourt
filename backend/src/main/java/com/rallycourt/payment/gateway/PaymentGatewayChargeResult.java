package com.rallycourt.payment.gateway;

public record PaymentGatewayChargeResult(
        boolean successful,
        String transactionReference,
        String message
) {
}
