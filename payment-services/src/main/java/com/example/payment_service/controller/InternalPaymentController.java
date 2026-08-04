package com.example.payment_service.controller;

import com.example.payment_service.dto.response.PaymentVerificationResponse;
import com.example.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments/internal")
@RequiredArgsConstructor
public class InternalPaymentController {

    private final PaymentService paymentService;

    @GetMapping("/verify-access")
    public ResponseEntity<PaymentVerificationResponse> verifyPayment(
            @RequestParam("userId") String userId,
            @RequestParam("targetId") String targetId,
            @RequestParam("targetType") String targetType
    ) {
        return ResponseEntity.ok(paymentService.verifyAccess(userId, targetId, targetType));
    }
}
