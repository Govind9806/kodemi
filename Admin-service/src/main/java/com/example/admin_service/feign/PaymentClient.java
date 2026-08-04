package com.example.admin_service.feign;

import com.example.admin_service.dto.request.PayoutRequest;
import com.example.admin_service.dto.request.ProcessPayoutRequest;
import com.example.admin_service.dto.response.TransactionHistoryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;

@FeignClient(name = "payment-service", fallbackFactory = com.example.admin_service.feign.fallback.PaymentClientFallbackFactory.class)
@Retry(name = "default")
public interface PaymentClient {
    @GetMapping("/api/v1/access/all/payouts")
    List<PayoutRequest> getAllPayouts(@RequestHeader("Authorization") String token);

    @PostMapping("/api/v1/access/payouts/process")
    String processPayoutRequest(
            @RequestBody ProcessPayoutRequest request,
            @RequestHeader("Authorization") String token);

    @PostMapping("/api/v1/access/payouts/process/{payoutId}")
    String processPayoutRequestByPath(
            @RequestHeader("Authorization") String token,
            @RequestParam("action") String action,
            @PathVariable("payoutId") String payoutId,
            @RequestParam(value = "remarks", required = false) String remarks
    );
    
    @GetMapping("/api/v1/access/all/transactions")
    TransactionHistoryResponse getTransactionHistory();
}
