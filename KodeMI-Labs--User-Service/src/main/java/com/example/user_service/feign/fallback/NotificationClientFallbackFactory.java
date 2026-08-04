package com.example.user_service.feign.fallback;

import com.example.user_service.feign.NotificationClient;
import com.example.user_service.notification.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationClientFallbackFactory implements FallbackFactory<NotificationClient> {

    @Override
    public NotificationClient create(Throwable cause) {
        return (serviceKey, request) -> log.warn("Notification service unavailable. Notification dropped. type={}, userId={}, error={}",
                request != null ? request.getType() : "null",
                request != null ? request.getUserId() : "null",
                cause.getMessage());
    }
}
