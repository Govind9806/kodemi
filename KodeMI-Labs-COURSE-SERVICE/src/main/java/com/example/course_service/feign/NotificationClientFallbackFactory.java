package com.example.course_service.feign;

import com.example.course_service.dto.request.BroadcastNotificationRequest;
import com.example.course_service.dto.request.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.Map;

@Component
public class NotificationClientFallbackFactory implements FallbackFactory<NotificationClient> {

    private static final Logger log = LoggerFactory.getLogger(NotificationClientFallbackFactory.class);

    @Override
    public NotificationClient create(Throwable cause) {
        return new NotificationClient() {
            @Override
            public Map<String, String> sendInternalNotification(String token, NotificationRequest request) {
                log.warn("NotificationClient.sendInternalNotification failed silently. Fallback triggered. Cause: {}", 
                        cause.getMessage(), cause);
                return Collections.emptyMap();
            }

            @Override
            public Map<String, Object> broadcastNotification(String token, BroadcastNotificationRequest request) {
                log.warn("NotificationClient.broadcastNotification failed silently. Fallback triggered. Cause: {}", 
                        cause.getMessage(), cause);
                return Collections.emptyMap();
            }
        };
    }
}
