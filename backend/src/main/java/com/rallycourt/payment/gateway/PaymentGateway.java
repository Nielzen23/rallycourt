package com.rallycourt.payment.gateway;

public interface PaymentGateway {

    PaymentGatewayChargeResult charge(PaymentGatewayChargeRequest request);
}
