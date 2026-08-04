package com.example.user_service.notification;

import com.example.user_service.feign.NotificationClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationPublisher {

    private final NotificationClient notificationClient;

    @Value("${internal.service.key:default-secret}")
    private String serviceKey;

    public NotificationPublisher(NotificationClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    public void publish(NotificationRequest request) {
        try {
            notificationClient.sendInternalNotification(serviceKey, request);
            log.info("Notification sent successfully. type={}, userId={}", request.getType(), request.getUserId());
        } catch (Exception ex) {
            log.error("Failed to send notification. type={}, userId={}", request.getType(), request.getUserId(), ex);
        }
    }
}
