package com.rallycourt.payment.service;

import com.rallycourt.payment.dto.ProcessPaymentRequest;
import com.rallycourt.payment.entity.Payment;

public interface PaymentService {

    Payment processPayment(ProcessPaymentRequest request);
}
