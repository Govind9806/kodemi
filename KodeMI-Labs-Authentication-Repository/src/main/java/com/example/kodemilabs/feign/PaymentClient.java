package com.example.kodemilabs.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "payment-service", fallbackFactory = PaymentClientFallbackFactory.class)
public interface PaymentClient {
    @PostMapping(value = "/api/v1/wallet/create", consumes = "application/json")
    void createWallet(@RequestHeader("Authorization") String token);
}
