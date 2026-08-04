package com.example.enrollment_progress_service.feign;

import com.example.enrollment_progress_service.dto.response.LearnerProfileDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", fallbackFactory = UserClientFallbackFactory.class)
public interface UserClient {
    @GetMapping("/api/v1/learner/details/{id}")
    LearnerProfileDTO getLearnerDetails(@PathVariable("id") String id);
}
