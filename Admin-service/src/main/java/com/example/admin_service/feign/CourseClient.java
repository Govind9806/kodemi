package com.example.admin_service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import java.util.Map;

@FeignClient(name = "course-service", fallbackFactory = com.example.admin_service.feign.fallback.CourseClientFallbackFactory.class)
@Retry(name = "default")
public interface CourseClient {

    @PutMapping("/api/v1/course/review/{courseId}")
    Map<String, Object> reviewCourse(
            @RequestHeader("Authorization") String token,
            @PathVariable("courseId") String courseId,
            @RequestBody com.example.admin_service.dto.request.CourseModerationRequest request);

    @GetMapping("/api/v1/course/admin/get-all-courses")
    List<com.example.admin_service.dto.response.CourseResponseDTO> getAllCoursesAdmin(
            @RequestHeader("Authorization") String token);
}
