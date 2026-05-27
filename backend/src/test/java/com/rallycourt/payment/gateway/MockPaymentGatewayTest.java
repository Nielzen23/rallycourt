package com.rallycourt.payment.gateway;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MockPaymentGatewayTest {

    private final MockPaymentGateway mockPaymentGateway = new MockPaymentGateway();

    @Test
    void chargeApprovesNormalPayment() {
        PaymentGatewayChargeResult result = mockPaymentGateway.charge(
                new PaymentGatewayChargeRequest(10L, BigDecimal.valueOf(750), null)
        );

        assertTrue(result.successful());
        assertNotNull(result.transactionReference());
    }

    @Test
    void chargeDeclinesMockDeclineToken() {
        PaymentGatewayChargeResult result = mockPaymentGateway.charge(
                new PaymentGatewayChargeRequest(10L, BigDecimal.valueOf(750), "mock-decline")
        );

        assertFalse(result.successful());
    }
}
