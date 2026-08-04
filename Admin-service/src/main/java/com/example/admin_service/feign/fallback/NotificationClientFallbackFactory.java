package com.example.admin_service.feign.fallback;

import com.example.admin_service.feign.NotificationClient;
import com.example.admin_service.dto.notification.BroadcastNotificationRequest;
import com.example.admin_service.dto.notification.NotificationRequest;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
public class NotificationClientFallbackFactory implements FallbackFactory<NotificationClient> {
    @Override
    public NotificationClient create(Throwable cause) {
        return new NotificationClient() {
            @Override
            public Map<String, String> sendInternalNotification(String token, NotificationRequest request) {
                log.error("NotificationClient sendInternalNotification failed (failing silent): {}", cause.getMessage(), cause);
                return Collections.emptyMap();
            }

            @Override
            public Map<String, String> broadcastNotification(String token, BroadcastNotificationRequest request) {
                log.error("NotificationClient broadcastNotification failed (failing silent): {}", cause.getMessage(), cause);
                return Collections.emptyMap();
            }
        };
    }
}
