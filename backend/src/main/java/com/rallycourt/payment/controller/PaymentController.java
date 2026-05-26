package com.rallycourt.payment.controller;

import com.rallycourt.activity.annotation.ActivityLogAnnotation;
import com.rallycourt.payment.dto.ProcessPaymentRequest;
import com.rallycourt.payment.entity.Payment;
import com.rallycourt.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process")
    @PreAuthorize("isAuthenticated()")
    @ActivityLogAnnotation("PAYMENT_SUCCESS")
    public ResponseEntity<Payment> processPayment(@Valid @RequestBody ProcessPaymentRequest request) {
        return ResponseEntity.ok(paymentService.processPayment(request));
    }
}
