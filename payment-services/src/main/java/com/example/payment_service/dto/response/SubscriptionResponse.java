package com.example.payment_service.dto.response;

import com.example.payment_service.enums.SubscriptionStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
public class SubscriptionResponse {

    private String subscriptionId;
    private String userId;
    private String planId;
    private SubscriptionStatus status;
    private Boolean autoRenew;
    private String paymentId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
