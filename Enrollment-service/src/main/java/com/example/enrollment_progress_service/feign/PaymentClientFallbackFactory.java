package com.example.enrollment_progress_service.feign;

import com.example.enrollment_progress_service.dto.response.PaymentVerificationResponse;
import com.example.enrollment_progress_service.exception.DownstreamServiceException;
import com.example.enrollment_progress_service.feign.PaymentClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PaymentClientFallbackFactory implements FallbackFactory<PaymentClient> {
    @Override
    public PaymentClient create(Throwable cause) {
        return new PaymentClient() {
            @Override
            public PaymentVerificationResponse verifyPayment(String userId, String targetId, String targetType) {
                log.error("Payment service call failed for verifyPayment with userId: {}, targetId: {}, targetType: {}. Reason: {}", 
                    userId, targetId, targetType, cause.getMessage(), cause);
                throw new DownstreamServiceException("Payment service is currently unavailable. Please try again later.", cause);
            }
        };
    }
}
