package com.example.enrollment_progress_service.service.notification;

import com.example.enrollment_progress_service.client.notification.NotificationClient;
import com.example.enrollment_progress_service.dto.notification.NotificationRequest;
import com.example.enrollment_progress_service.dto.notification.BroadcastNotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class NotificationPublisher {

    private final NotificationClient notificationClient;
    private final String serviceKey;

    public NotificationPublisher(
            NotificationClient notificationClient,
            @Value("${internal.service.key:default-secret}") String serviceKey) {
        this.notificationClient = notificationClient;
        this.serviceKey = serviceKey;
    }

    public void publish(NotificationRequest request) {
        CompletableFuture.runAsync(() -> {
            try {
                notificationClient.sendInternalNotification(serviceKey, request);
                log.info("Notification sent successfully. type={}, userId={}, referenceId={}",
                        request.getType(), request.getUserId(), request.getReferenceId());
            } catch (Exception ex) {
                log.error("Failed to send notification. type={}, userId={}, referenceId={}",
                        request.getType(), request.getUserId(), request.getReferenceId(), ex);
            }
        });
    }

    public void publishBroadcast(BroadcastNotificationRequest request) {
        CompletableFuture.runAsync(() -> {
            try {
                notificationClient.broadcastNotification(serviceKey, request);
                log.info("Broadcast notification sent successfully. type={}, targetRole={}, referenceId={}",
                        request.getType(), request.getTargetRole(), request.getReferenceId());
            } catch (Exception ex) {
                log.error("Failed to send broadcast notification. type={}, targetRole={}, referenceId={}",
                        request.getType(), request.getTargetRole(), request.getReferenceId(), ex);
            }
        });
    }

    public void publishToUsers(java.util.List<String> userIds, NotificationRequest request) {
        if (userIds == null || userIds.isEmpty()) return;
        CompletableFuture.runAsync(() -> {
            for (String userId : userIds) {
                try {
                    request.setUserId(userId);
                    notificationClient.sendInternalNotification(serviceKey, request);
                    log.info("Notification sent successfully. type={}, userId={}, referenceId={}",
                            request.getType(), userId, request.getReferenceId());
                } catch (Exception ex) {
                    log.error("Failed to send notification. type={}, userId={}, referenceId={}",
                            request.getType(), userId, request.getReferenceId(), ex);
                }
            }
        });
    }
}
