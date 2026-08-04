package com.example.enrollment_progress_service.feign;

import com.example.enrollment_progress_service.dto.response.EnrollmentItemInfoResponse;
import com.example.enrollment_progress_service.feign.CourseClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "course-service", fallbackFactory = CourseClientFallbackFactory.class)
public interface CourseClient {
    @GetMapping("/api/v1/course/internal/enrollment-info/{courseId}")
    EnrollmentItemInfoResponse getCourseEnrollmentInfo(@PathVariable("courseId") String courseId);

    @GetMapping("/api/v1/course/get-detail/{courseId}")
    com.example.enrollment_progress_service.dto.response.CourseResponseDTO getCourseDetail(@PathVariable("courseId") String courseId);

    @GetMapping("/api/v1/reviews/course/{courseId}")
    java.util.List<com.example.enrollment_progress_service.dto.response.ReviewResponseDTO> getReviewsByCourse(@PathVariable("courseId") String courseId);
}
