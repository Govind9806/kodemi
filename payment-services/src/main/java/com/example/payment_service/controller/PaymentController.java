package com.example.payment_service.controller;
import com.example.payment_service.dto.request.PaymentOrderRequest;
import com.example.payment_service.dto.response.PaymentOrderResponse;
import com.example.payment_service.dto.request.PaymentVerifyRequest;
import com.example.payment_service.model.Payment;
import com.example.payment_service.service.PaymentService;
import com.example.payment_service.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

@Slf4j
@RestController
@RequestMapping("/api/v1/payment")
@RateLimiter(name = "paymentEndpoint")
public class PaymentController {
    private final PaymentService paymentService;
    private final JwtUtil jwtUtil;

    public PaymentController(PaymentService paymentService, JwtUtil jwtUtil) {
        this.paymentService = paymentService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(
            @RequestBody PaymentOrderRequest request,
            @RequestHeader("Authorization") String token) {
        try {
            String userId = jwtUtil.extractUserId(token);
            log.info("Creating order for userId: {}", userId);
            request.setUserid(userId);
            PaymentOrderResponse response = paymentService.createOrder(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Invalid token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token");
        }
    }
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(
            @RequestBody PaymentVerifyRequest request,
            @RequestHeader("Authorization") String token) {
        try {
            jwtUtil.extractUserId(token);
            log.info("Verifying payment for orderId: {}", request.getRazorpayOrderId());
            String result = paymentService.verifyPayment(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Invalid token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token");
        }
    }
    @GetMapping("/status/{orderId}")
    public ResponseEntity<?> getPaymentStatus(
            @PathVariable String orderId,
            @RequestHeader("Authorization") String token) {
        try {
            jwtUtil.extractUserId(token); 
            Payment payment = paymentService.getPaymentStatus(orderId);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            log.error("Invalid token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token");
        }
    }
}
