package com.example.payment_service.dto.request;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class BroadcastNotificationRequest {
    private String title;
    private String message;
    private String type;
    private List<String> channels;
    private String targetRole;
    private String sendMode;
    private String referenceId;
}

