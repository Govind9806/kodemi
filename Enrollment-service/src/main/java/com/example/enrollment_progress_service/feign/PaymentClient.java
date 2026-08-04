package com.example.enrollment_progress_service.feign;

import com.example.enrollment_progress_service.dto.response.PaymentVerificationResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "payment-service", fallbackFactory = PaymentClientFallbackFactory.class)
public interface PaymentClient {
    @GetMapping("/api/v1/payments/internal/verify-access")
    PaymentVerificationResponse verifyPayment(
            @RequestParam("userId") String userId,
            @RequestParam("targetId") String targetId,
            @RequestParam("targetType") String targetType
    );
}
