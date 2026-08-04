package com.example.payment_service.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationRequest {
    private String userId;
    private String title;
    private String message;
    private String type;
    private String channel;
    private java.util.List<String> channels;
    private String referenceId;
}

