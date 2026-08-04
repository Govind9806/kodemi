package com.example.kodemilabs.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentClientFallbackFactory implements FallbackFactory<PaymentClient> {
    @Override
    public PaymentClient create(Throwable cause) {
        return token -> log.error("PaymentClient.createWallet failed (fail-silent). Downstream payment-service is unavailable. Cause: {}", cause.getMessage(), cause);
    }
}
