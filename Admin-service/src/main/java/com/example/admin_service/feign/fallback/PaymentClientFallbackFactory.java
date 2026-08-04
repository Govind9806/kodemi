package com.example.admin_service.feign.fallback;

import com.example.admin_service.feign.PaymentClient;
import com.example.admin_service.dto.request.PayoutRequest;
import com.example.admin_service.dto.request.ProcessPayoutRequest;
import com.example.admin_service.dto.response.TransactionHistoryResponse;
import com.example.admin_service.exceptions.DownstreamServiceException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
@Component
public class PaymentClientFallbackFactory implements FallbackFactory<PaymentClient> {
    @Override
    public PaymentClient create(Throwable cause) {
        return new PaymentClient() {
            @Override
            public List<PayoutRequest> getAllPayouts(String token) {
                log.error("PaymentClient getAllPayouts failed: {}", cause.getMessage(), cause);
                throw new DownstreamServiceException("Payment service is currently unavailable.", cause);
            }

            @Override
            public String processPayoutRequest(ProcessPayoutRequest request, String token) {
                log.error("PaymentClient processPayoutRequest failed: {}", cause.getMessage(), cause);
                throw new DownstreamServiceException("Payment service is currently unavailable.", cause);
            }

            @Override
            public String processPayoutRequestByPath(String token, String action, String payoutId, String remarks) {
                log.error("PaymentClient processPayoutRequestByPath failed: {}", cause.getMessage(), cause);
                throw new DownstreamServiceException("Payment service is currently unavailable.", cause);
            }

            @Override
            public TransactionHistoryResponse getTransactionHistory() {
                log.error("PaymentClient getTransactionHistory failed: {}", cause.getMessage(), cause);
                throw new DownstreamServiceException("Payment service is currently unavailable.", cause);
            }
        };
    }
}
