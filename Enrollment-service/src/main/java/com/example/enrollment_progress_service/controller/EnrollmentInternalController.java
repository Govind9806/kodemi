package com.example.enrollment_progress_service.controller;

import com.example.enrollment_progress_service.dto.request.PaymentSuccessRequest;
import com.example.enrollment_progress_service.dto.response.EnrollmentResponse;
import com.example.enrollment_progress_service.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enrollments/internal")
@RequiredArgsConstructor
@Slf4j
public class EnrollmentInternalController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/payment-success")
    public ResponseEntity<EnrollmentResponse> markPaymentSuccess(@Valid @RequestBody PaymentSuccessRequest request) {
        log.info("Received internal payment success callback for orderId={}", request.getOrderId());
        EnrollmentResponse response = enrollmentService.activateEnrollmentAfterPayment(request);
        return ResponseEntity.ok(response);
    }
}
