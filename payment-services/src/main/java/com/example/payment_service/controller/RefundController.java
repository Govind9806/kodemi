package com.example.payment_service.controller;

import com.example.payment_service.component.RequiresRole;
import com.example.payment_service.service.RefundService;
import com.example.payment_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

@Slf4j
@RestController
@RequestMapping("/api/v1/refund")
@RateLimiter(name = "standardEndpoint")
public class RefundController {

    private final RefundService refundService;
    private final JwtUtil jwtUtil;

    public RefundController(RefundService refundService, JwtUtil jwtUtil) {
        this.refundService = refundService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/process/{transactionId}")
    @RequiresRole({"PAYMENT_ADMIN", "SUPER_ADMIN"})
    public ResponseEntity<String> processRefund(
            @PathVariable String transactionId,
            @RequestParam String reason,
            @RequestHeader("Authorization") String token) {
        
        try {
            String adminId = jwtUtil.extractUserId(token);
            refundService.processRefund(transactionId, adminId, reason);
            return ResponseEntity.ok("Refund processed successfully");
        } catch (Exception e) {
            log.error("Error processing refund: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to process refund: " + e.getMessage());
        }
    }
}