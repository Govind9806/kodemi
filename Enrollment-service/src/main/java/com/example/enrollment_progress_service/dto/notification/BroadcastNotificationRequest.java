package com.example.enrollment_progress_service.dto.notification;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class BroadcastNotificationRequest {
    private String title;
    private String message;
    private NotificationType type;
    private List<NotificationChannel> channels;
    private String targetRole;
    private String sendMode;
    private String referenceId;
}
