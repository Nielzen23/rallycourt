package com.rallycourt.payment.gateway;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MockPaymentGateway implements PaymentGateway {

    private static final String DECLINE_TOKEN = "mock-decline";
    private static final Logger LOGGER = LoggerFactory.getLogger(MockPaymentGateway.class);

    @Override
    public PaymentGatewayChargeResult charge(PaymentGatewayChargeRequest request) {
        LOGGER.info("Mock payment gateway charge requested for reservationId {}", request.reservationId());
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            LOGGER.warn("Mock payment gateway rejected reservationId {} due to invalid amount", request.reservationId());
            return new PaymentGatewayChargeResult(false, null, "Payment gateway rejected the transaction");
        }

        if (StringUtils.hasText(request.paymentMethodToken())
                && DECLINE_TOKEN.equalsIgnoreCase(request.paymentMethodToken().trim())) {
            LOGGER.info("Mock payment gateway declined reservationId {} due to test decline token", request.reservationId());
            return new PaymentGatewayChargeResult(false, null, "Payment gateway rejected the transaction");
        }

        LOGGER.info("Mock payment gateway approved reservationId {}", request.reservationId());
        return new PaymentGatewayChargeResult(
                true,
                "mock-" + request.reservationId() + "-" + System.currentTimeMillis(),
                "Payment processed successfully"
        );
    }
}
