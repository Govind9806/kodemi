package com.example.course_service.feign;

import com.example.course_service.dto.response.AccessCheckResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "enrollment-progress-service", fallbackFactory = EnrollmentClientFallbackFactory.class)
public interface EnrollmentClient {

    @GetMapping("/api/v1/enrollments/internal/access-check")
    AccessCheckResponse checkAccess(
            @RequestParam("userId") String userId,
            @RequestParam("targetId") String targetId,
            @RequestParam("targetType") String targetType
    );

    @GetMapping("/api/v1/enrollments/internal/course/{courseId}/learners")
    java.util.List<String> getEnrolledLearners(@org.springframework.web.bind.annotation.PathVariable("courseId") String courseId);

    @GetMapping("/api/v1/enrollments/my")
    java.util.List<com.example.course_service.dto.response.UserEnrollmentResponse> getUserEnrollmentsInternal(
            @org.springframework.web.bind.annotation.RequestHeader("Authorization") String token);
}
