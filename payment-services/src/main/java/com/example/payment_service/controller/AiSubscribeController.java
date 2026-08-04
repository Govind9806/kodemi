package com.example.payment_service.controller;

import com.example.payment_service.dto.request.CreateSubscriptionRequest;
import com.example.payment_service.dto.response.SubscriptionResponse;
import com.example.payment_service.dto.request.UpdateSubscriptionRequest;
import com.example.payment_service.service.AiSubscribeService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/subscriptions")
public class AiSubscribeController {

    private final AiSubscribeService subscribeService;

    public AiSubscribeController(AiSubscribeService subscribeService) {
        this.subscribeService = subscribeService;
    }

    @PostMapping("/buy")
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @Valid @RequestBody CreateSubscriptionRequest request,
            @RequestHeader("Authorization") String token) {
        log.info("Received request to create a subscription");
        SubscriptionResponse response = subscribeService.createSubscription(request, token);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{subscriptionId}")
    public ResponseEntity<SubscriptionResponse> getSubscription(
            @PathVariable String subscriptionId) {
        return ResponseEntity.ok(subscribeService.getSubscriptionById(subscriptionId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SubscriptionResponse>> getSubscriptionsByUser(
            @PathVariable String userId) {
        return ResponseEntity.ok(subscribeService.getSubscriptionsByUserId(userId));
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<SubscriptionResponse> getActiveSubscription(
            @PathVariable String userId) {
        return ResponseEntity.ok(subscribeService.getActiveSubscriptionByUserId(userId));
    }

    @GetMapping("/user/{userId}/status")
    public ResponseEntity<Map<String, Boolean>> checkSubscriptionStatus(
            @PathVariable String userId) {
        boolean isActive = subscribeService.isSubscriptionActive(userId);
        return ResponseEntity.ok(Map.of("active", isActive));
    }

    @GetMapping("/all")
    public ResponseEntity<List<SubscriptionResponse>> getAllSubscriptions() {
        return ResponseEntity.ok(subscribeService.getAllSubscriptions());
    }

    @PutMapping("/update/{subscriptionId}")
    public ResponseEntity<SubscriptionResponse> updateSubscription(
            @PathVariable String subscriptionId,
            @RequestBody UpdateSubscriptionRequest request) {
        return ResponseEntity.ok(subscribeService.updateSubscription(subscriptionId, request));
    }

    @PostMapping("/{subscriptionId}/cancel")
    public ResponseEntity<SubscriptionResponse> cancelSubscription(
            @PathVariable String subscriptionId) {
        return ResponseEntity.ok(subscribeService.cancelSubscription(subscriptionId));
    }

    @PostMapping("/{subscriptionId}/renew")
    public ResponseEntity<SubscriptionResponse> renewSubscription(
            @PathVariable String subscriptionId,
            @RequestParam LocalDateTime newEndDate) {
        return ResponseEntity.ok(subscribeService.renewSubscription(subscriptionId, newEndDate));
    }

    @DeleteMapping("/delete/{subscriptionId}")
    public ResponseEntity<Void> deleteSubscription(@PathVariable String subscriptionId) {
        subscribeService.deleteSubscription(subscriptionId);
        return ResponseEntity.noContent().build();
    }
}
