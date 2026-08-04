package com.example.enrollment_progress_service.feign;

import com.example.enrollment_progress_service.dto.request.NotificationRequest;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(
        name = "notification-service",
        contextId = "enrollmentNotificationClient",
        configuration = NotificationClient.Configuration.class,
        fallbackFactory = NotificationClientFallbackFactory.class
)
public interface NotificationClient {

    class Configuration {

        @Value("${internal.service.key:default-secret}")
        private String internalServiceKey;

        @Bean
        public RequestInterceptor requestInterceptor() {
            return requestTemplate ->
                    requestTemplate.header("X-Internal-Service-Key", internalServiceKey);
        }
    }

    @PostMapping("/api/v1/notifications/internal/send")
    Map<String, String> sendInternalNotification(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody NotificationRequest request
    );

    @PostMapping("/api/v1/notifications/send")
    Map<String, String> sendNotification(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody NotificationRequest request
    );
}