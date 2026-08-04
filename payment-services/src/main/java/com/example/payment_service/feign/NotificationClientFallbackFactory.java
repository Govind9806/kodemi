package com.example.payment_service.feign;

import com.example.payment_service.dto.request.BroadcastNotificationRequest;
import com.example.payment_service.dto.request.NotificationRequest;
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
        log.error("Notification service fallback triggered due to: {}", cause.getMessage(), cause);
        return new NotificationClient() {
            @Override
            public Map<String, String> sendInternalNotification(String token, NotificationRequest request) {
                log.warn("Fallback: sendInternalNotification failed-silent. Returning empty map.");
                return Collections.emptyMap();
            }

            @Override
            public Map<String, Object> broadcastNotification(String token, BroadcastNotificationRequest request) {
                log.warn("Fallback: broadcastNotification failed-silent. Returning empty map.");
                return Collections.emptyMap();
            }
        };
    }
}
