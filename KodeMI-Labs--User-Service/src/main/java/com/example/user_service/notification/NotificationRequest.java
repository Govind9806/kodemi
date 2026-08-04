package com.example.user_service.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private String userId;
    private String title;
    private String message;
    private NotificationType type;
    private List<NotificationChannel> channels;
    private String referenceId;
    private String referenceType;
    private String email;
    private String phoneNumber;
}
