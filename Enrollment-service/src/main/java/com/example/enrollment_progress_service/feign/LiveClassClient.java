package com.example.enrollment_progress_service.feign;

import com.example.enrollment_progress_service.dto.response.EnrollmentItemInfoResponse;
import com.example.enrollment_progress_service.feign.LiveClassClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "live-classes-service", fallbackFactory = LiveClassClientFallbackFactory.class)
public interface LiveClassClient {
    @GetMapping("/api/v1/conferences/internal/enrollment-info/{conferenceId}")
    EnrollmentItemInfoResponse getConferenceEnrollmentInfo(@PathVariable("conferenceId") String conferenceId);
}
