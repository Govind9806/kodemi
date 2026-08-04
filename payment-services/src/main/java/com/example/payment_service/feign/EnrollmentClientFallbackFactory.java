package com.example.payment_service.feign;

import com.example.payment_service.dto.request.PaymentSuccessRequest;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class EnrollmentClientFallbackFactory implements FallbackFactory<EnrollmentClient> {

    @Override
    public EnrollmentClient create(Throwable cause) {
        log.error("EnrollmentClient fallback triggered due to: {}", cause.getMessage(), cause);
        return new EnrollmentClient() {
            @Override
            public void markPaymentSuccess(PaymentSuccessRequest request) {
                log.warn("Fallback: markPaymentSuccess failed-silent for orderId: {}", request != null ? request.getOrderId() : null);
            }
        };
    }
}
