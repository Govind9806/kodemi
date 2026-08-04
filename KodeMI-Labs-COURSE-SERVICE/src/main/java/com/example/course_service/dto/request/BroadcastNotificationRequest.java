package com.example.course_service.dto.request;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class BroadcastNotificationRequest {
    private String title;
    private String message;
    private String type;
    private List<com.example.course_service.dto.notification.NotificationChannel> channels;
    private String targetRole;
    private String sendMode;
    private String referenceId;
}

