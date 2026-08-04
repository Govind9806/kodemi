package com.example.enrollment_progress_service.feign;

import com.example.enrollment_progress_service.dto.response.LearnerProfileDTO;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {
    @Override
    public UserClient create(Throwable cause) {
        return new UserClient() {
            @Override
            public LearnerProfileDTO getLearnerDetails(String id) {
                log.warn("Fallback: failed to fetch learner details for id: {}. Reason: {}", id, cause.getMessage());
                return null;
            }
        };
    }
}
