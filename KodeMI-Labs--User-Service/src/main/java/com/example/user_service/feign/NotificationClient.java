package com.example.user_service.feign;

import com.example.user_service.notification.NotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "notification-service",
        fallbackFactory = com.example.user_service.feign.fallback.NotificationClientFallbackFactory.class)
public interface NotificationClient {

    @PostMapping("/api/v1/notifications/internal/send")
    void sendInternalNotification(
            @RequestHeader("X-Internal-Service-Key") String serviceKey,
            @RequestBody NotificationRequest request
    );
}
