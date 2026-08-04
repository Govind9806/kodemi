package com.example.enrollment_progress_service.client.notification;

import com.example.enrollment_progress_service.dto.notification.BroadcastNotificationRequest;
import com.example.enrollment_progress_service.dto.notification.NotificationRequest;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "notification-service", url = "${notification.service.url:http://localhost:8085}")
public interface NotificationClient {

    @PostMapping("/api/v1/notifications/internal/send")
    Map<String, String> sendInternalNotification(
            @RequestHeader("X-Internal-Service-Key") String serviceKey,
            @RequestBody NotificationRequest request
    );

    @PostMapping("/api/v1/notifications/internal/broadcast")
    Map<String, Object> broadcastNotification(
            @RequestHeader("X-Internal-Service-Key") String serviceKey,
            @RequestBody BroadcastNotificationRequest request
    );
}
