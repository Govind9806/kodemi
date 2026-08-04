package com.example.payment_service.service;

import com.example.payment_service.dto.request.CreateSubscriptionRequest;
import com.example.payment_service.dto.response.SubscriptionResponse;
import com.example.payment_service.dto.request.UpdateSubscriptionRequest;
import com.example.payment_service.enums.SubscriptionStatus;
import com.example.payment_service.exception.DuplicateSubscriptionCreationException;
import com.example.payment_service.exception.ResourceNotFoundException;
import com.example.payment_service.model.AiSubscribe;
import com.example.payment_service.repository.AiSubscribeRepository;
import com.example.payment_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AiSubscribeService {

    private final AiSubscribeRepository repository;
    private final JwtUtil jwtUtil;
    private final com.example.payment_service.service.notification.NotificationPublisher notificationPublisher;

    public AiSubscribeService(AiSubscribeRepository repository, JwtUtil jwtUtil,
                              com.example.payment_service.service.notification.NotificationPublisher notificationPublisher) {
        this.repository = repository;
        this.jwtUtil = jwtUtil;
        this.notificationPublisher = notificationPublisher;
    }

    public SubscriptionResponse createSubscription(CreateSubscriptionRequest request, String token) {
        String userID = jwtUtil.extractUserId(token);
        List<AiSubscribe> list = repository.findByUserId(userID);
        if(!list.isEmpty()){
            throw new DuplicateSubscriptionCreationException("Subscription already exists! Please Renew the Subscription to continue.");
        }
        log.info("Creating subscription for user: {}", userID);

        AiSubscribe subscription = new AiSubscribe();
        subscription.setSubscriptionId(UUID.randomUUID().toString());
        subscription.setUserId(userID);
        subscription.setPlanId(request.getPlanId());
        subscription.setPaymentId(request.getPaymentId());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setAutoRenew(request.getAutoRenew());
        subscription.setStartDate(request.getStartDate());
        subscription.setEndDate(request.getEndDate());
        subscription.setCreatedAt(LocalDateTime.now());
        subscription.setUpdatedAt(LocalDateTime.now());

        AiSubscribe saved = repository.save(subscription);
        log.info("Subscription created with ID: {}", saved.getSubscriptionId());

        // Notify user of successful subscription
        try {
            com.example.payment_service.dto.notification.NotificationRequest notif = com.example.payment_service.dto.notification.NotificationRequest.builder()
                    .userId(userID)
                    .title("AI Subscription Activated 🤖")
                    .message("Your AI Subscription (Plan: " + saved.getPlanId() + ") is now active until " + saved.getEndDate() + ".")
                    .type(com.example.payment_service.dto.notification.NotificationType.PAYMENT_SUCCESSFUL)
                    .channels(java.util.List.of(com.example.payment_service.dto.notification.NotificationChannel.IN_APP, com.example.payment_service.dto.notification.NotificationChannel.EMAIL))
                    .referenceId(saved.getSubscriptionId())
                    .referenceType("AI_SUBSCRIPTION")
                    .build();
            notificationPublisher.publish(notif);
        } catch (Exception ex) {
            log.error("Failed to send AI subscription creation notification", ex);
        }

        return mapToResponse(saved);
    }

    public SubscriptionResponse getSubscriptionById(String subscriptionId) {
        log.info("Fetching subscription by ID: {}", subscriptionId);
        Optional<AiSubscribe> opt = repository.findById(subscriptionId);
        if (!opt.isPresent()) {
            log.warn("Subscription not found for ID: {}", subscriptionId);
            throw new ResourceNotFoundException("Subscription not found: " + subscriptionId);
        }
        AiSubscribe subscription = opt.get();
        log.info("Successfully retrieved subscription | ID: {}, userId: {}", subscription.getSubscriptionId(), subscription.getUserId());
        return mapToResponse(subscription);
    }

    public List<SubscriptionResponse> getSubscriptionsByUserId(String userId) {
        log.info("Fetching all subscriptions for userId: {}", userId);
        List<AiSubscribe> list = repository.findByUserId(userId);
        log.info("Found {} subscriptions for userId: {}", list != null ? list.size() : 0, userId);
        List<SubscriptionResponse> responses = new java.util.ArrayList<SubscriptionResponse>();
        for (AiSubscribe sub : list) {
            responses.add(mapToResponse(sub));
        }
        return responses;
    }

    public SubscriptionResponse getActiveSubscriptionByUserId(String userId) {
        log.info("Locating active subscription for userId: {}", userId);
        List<AiSubscribe> list = repository.findByUserId(userId);
        for (AiSubscribe sub : list) {
            if (sub.getStatus() == SubscriptionStatus.ACTIVE) {
                log.info("Found active subscription | ID: {}, planId: {}", sub.getSubscriptionId(), sub.getPlanId());
                return mapToResponse(sub);
            }
        }
        log.warn("No active subscription found for userId: {}", userId);
        throw new ResourceNotFoundException("No active subscription found for user: " + userId);
    }

    public List<SubscriptionResponse> getAllSubscriptions() {
        List<AiSubscribe> list = repository.findAll();
        List<SubscriptionResponse> responses = new java.util.ArrayList<SubscriptionResponse>();
        for (AiSubscribe sub : list) {
            responses.add(mapToResponse(sub));
        }
        return responses;
    }

    public SubscriptionResponse updateSubscription(String subscriptionId, UpdateSubscriptionRequest request) {
        log.info("Updating subscription: {}", subscriptionId);

        Optional<AiSubscribe> opt = repository.findById(subscriptionId);
        if (!opt.isPresent()) {
            throw new ResourceNotFoundException("Subscription not found: " + subscriptionId);
        }
        AiSubscribe subscription = opt.get();

        if (request.getPlanId() != null) {
            subscription.setPlanId(request.getPlanId());
        }
        if (request.getStatus() != null) {
            subscription.setStatus(request.getStatus());
        }
        if (request.getAutoRenew() != null) {
            subscription.setAutoRenew(request.getAutoRenew());
        }
        if (request.getEndDate() != null) {
            subscription.setEndDate(request.getEndDate());
        }
        subscription.setUpdatedAt(LocalDateTime.now());

        AiSubscribe updated = repository.save(subscription);
        log.info("Subscription updated: {}", subscriptionId);

        return mapToResponse(updated);
    }

    public SubscriptionResponse cancelSubscription(String subscriptionId) {
        log.info("Cancelling subscription: {}", subscriptionId);

        Optional<AiSubscribe> opt = repository.findById(subscriptionId);
        if (!opt.isPresent()) {
            throw new ResourceNotFoundException("Subscription not found: " + subscriptionId);
        }
        AiSubscribe subscription = opt.get();

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setAutoRenew(false);
        subscription.setUpdatedAt(LocalDateTime.now());

        AiSubscribe cancelled = repository.save(subscription);
        log.info("Subscription cancelled: {}", subscriptionId);

        // Notify user of subscription cancellation
        try {
            com.example.payment_service.dto.notification.NotificationRequest notif = com.example.payment_service.dto.notification.NotificationRequest.builder()
                    .userId(cancelled.getUserId())
                    .title("AI Subscription Cancelled ⏸️")
                    .message("Your AI Subscription has been cancelled. Auto-renewal is disabled, but you will retain access until " + cancelled.getEndDate() + ".")
                    .type(com.example.payment_service.dto.notification.NotificationType.PAYMENT_FAILED)
                    .channels(java.util.List.of(com.example.payment_service.dto.notification.NotificationChannel.IN_APP))
                    .referenceId(cancelled.getSubscriptionId())
                    .referenceType("AI_SUBSCRIPTION")
                    .build();
            notificationPublisher.publish(notif);
        } catch (Exception ex) {
            log.error("Failed to send AI subscription cancellation notification", ex);
        }

        return mapToResponse(cancelled);
    }

    public SubscriptionResponse renewSubscription(String subscriptionId, LocalDateTime newEndDate) {
        log.info("Renewing subscription: {}", subscriptionId);

        Optional<AiSubscribe> opt = repository.findById(subscriptionId);
        if (!opt.isPresent()) {
            throw new ResourceNotFoundException("Subscription not found: " + subscriptionId);
        }
        AiSubscribe subscription = opt.get();

        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setEndDate(newEndDate);
        subscription.setUpdatedAt(LocalDateTime.now());

        AiSubscribe renewed = repository.save(subscription);
        log.info("Subscription renewed: {}", subscriptionId);

        // Notify user of subscription renewal
        try {
            com.example.payment_service.dto.notification.NotificationRequest notif = com.example.payment_service.dto.notification.NotificationRequest.builder()
                    .userId(renewed.getUserId())
                    .title("AI Subscription Renewed 🤖")
                    .message("Your AI Subscription has been successfully renewed until " + renewed.getEndDate() + ".")
                    .type(com.example.payment_service.dto.notification.NotificationType.PAYMENT_SUCCESSFUL)
                    .channels(java.util.List.of(com.example.payment_service.dto.notification.NotificationChannel.IN_APP, com.example.payment_service.dto.notification.NotificationChannel.EMAIL))
                    .referenceId(renewed.getSubscriptionId())
                    .referenceType("AI_SUBSCRIPTION")
                    .build();
            notificationPublisher.publish(notif);
        } catch (Exception ex) {
            log.error("Failed to send AI subscription renewal notification", ex);
        }

        return mapToResponse(renewed);
    }

    public void deleteSubscription(String subscriptionId) {
        log.info("Deleting subscription: {}", subscriptionId);
        repository.deleteById(subscriptionId);
    }

    public boolean isSubscriptionActive(String userId) {
        List<AiSubscribe> list = repository.findByUserId(userId);
        for (AiSubscribe sub : list) {
            if (sub.getStatus() == SubscriptionStatus.ACTIVE && sub.getEndDate().isAfter(LocalDateTime.now())) {
                return true;
            }
        }
        return false;
    }

    private SubscriptionResponse mapToResponse(AiSubscribe subscription) {
        SubscriptionResponse response = new SubscriptionResponse();
        response.setSubscriptionId(subscription.getSubscriptionId());
        response.setUserId(subscription.getUserId());
        response.setPlanId(subscription.getPlanId());
        response.setStatus(subscription.getStatus());
        response.setAutoRenew(subscription.getAutoRenew());
        response.setPaymentId(subscription.getPaymentId());
        response.setStartDate(subscription.getStartDate());
        response.setEndDate(subscription.getEndDate());
        response.setCreatedAt(subscription.getCreatedAt());
        response.setUpdatedAt(subscription.getUpdatedAt());
        return response;
    }
}
