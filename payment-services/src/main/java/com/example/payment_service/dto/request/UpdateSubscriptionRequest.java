package com.example.payment_service.dto.request;

import com.example.payment_service.enums.SubscriptionStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
public class UpdateSubscriptionRequest {

    private String planId;
    private SubscriptionStatus status;
    private Boolean autoRenew;
    private LocalDateTime endDate;
}
