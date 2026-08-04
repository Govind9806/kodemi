package com.example.enrollment_progress_service.feign;

import com.example.enrollment_progress_service.dto.request.NotificationRequest;
import com.example.enrollment_progress_service.feign.NotificationClient;
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
                log.warn("Notification service call failed for sendInternalNotification. Failing silent. Reason: {}", cause.getMessage(), cause);
                return Collections.emptyMap();
            }

            @Override
            public Map<String, String> sendNotification(String token, NotificationRequest request) {
                log.warn("Notification service call failed for sendNotification. Failing silent. Reason: {}", cause.getMessage(), cause);
                return Collections.emptyMap();
            }
        };
    }
}
