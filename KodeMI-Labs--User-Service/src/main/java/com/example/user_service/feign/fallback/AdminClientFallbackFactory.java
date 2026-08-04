package com.example.user_service.feign.fallback;

import com.example.user_service.dto.response.TrainerResponseDTO;
import com.example.user_service.feign.AdminClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AdminClientFallbackFactory implements FallbackFactory<AdminClient> {
    @Override
    public AdminClient create(Throwable cause) {
        return trainerDetails -> log.error("Optional Admin service call failed silently. Error: {}", cause.getMessage(), cause);
    }
}
