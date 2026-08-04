package com.example.payment_service.feign;

import com.example.payment_service.dto.request.PaymentSuccessRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "enrollment-progress-service",
        contextId = "paymentEnrollmentClient",
        fallbackFactory = EnrollmentClientFallbackFactory.class
)
public interface EnrollmentClient {

    @PostMapping("/api/v1/enrollments/internal/payment-success")
    void markPaymentSuccess(@RequestBody PaymentSuccessRequest request);
}
